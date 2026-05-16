package com.example.dacs3.dashboard

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.dacs3.MainActivity
import com.example.dacs3.R
import com.example.dacs3.connectDB.*
import com.example.dacs3.RS.OutfitRecommendationService
import com.google.ai.client.generativeai.GenerativeModel
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.coroutines.resume

class DailyOutfitWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // 1. LẤY TỌA ĐỘ VÀ THỜI TIẾT THỰC TẾ
            val location = suspendCancellableCoroutine<Pair<Double, Double>?> { continuation ->
                LocationHelper.getCurrentLocation(
                    context = applicationContext,
                    onLocationFetched = { lat, lon -> continuation.resume(Pair(lat, lon)) },
                    onFailed = { continuation.resume(null) }
                )
            }

            var weatherCondition = "Clear"
            var temp = "25°C"
            if (location != null) {
                val weatherApiKey = com.example.dacs3.BuildConfig.WEATHER_API_KEY
                val response = RetrofitInstance.api.getWeatherByLocation(
                    location.first,
                    location.second,
                    weatherApiKey
                )
                weatherCondition = response.weather.firstOrNull()?.main ?: "Clear"

                // Giả sử API trả về Kelvin, đổi sang độ C (nếu API trả thẳng độ C thì bỏ qua bước trừ 273.15)
                val tempC = (response.main.temp - 273.15).toInt()
                temp = "$tempC°C"
            }

            // 2. LẤY LỊCH TRÌNH TỪ GOOGLE CALENDAR
            val todayEvents = getTodaysCalendarEvents(context)
            val scheduleContext = if (todayEvents.isNotEmpty()) {
                "Today's events: $todayEvents"
            } else {
                "No specific events today, just a regular day."
            }

            // 2.5 LẤY TỦ ĐỒ VÀ GỢI Ý TỪ LOCAL RS (Hybrid Approach)
            val userId = supabase.auth.currentUserOrNull()?.id
            var localOutfitContext = ""
            if (userId != null) {
                try {
                    val profile = supabase.from("profiles").select { filter { eq("id", userId) } }.decodeSingle<Profile>()
                    val wardrobe = supabase.from("clothes").select { filter { eq("user_id", userId); neq("status", "unactive") } }.decodeList<ClothingItem>()
                    val history = supabase.from("usage_history").select { filter { eq("user_id", userId) } }.decodeList<UsageHistoryDbModel>()
                    
                    val outfitIds = history.map { it.outfitId }
                    val frequencyMap = if (outfitIds.isNotEmpty()) {
                        val outfitItems = supabase.from("outfit_items").select { filter { isIn("outfit_id", outfitIds) } }.decodeList<OutfitItemDbModel>()
                        outfitItems.groupingBy { it.clothingId }.eachCount()
                    } else emptyMap()

                    // FETCH FEEDBACK (New)
                    val feedbackList = try {
                        supabase.from("clothing_feedback")
                            .select { filter { eq("user_id", userId) } }
                            .decodeList<ClothingFeedback>()
                    } catch (e: Exception) { emptyList<ClothingFeedback>() }
                    val feedbackMap = feedbackList.associate { it.clothingId to it.rating }

                    val month = Calendar.getInstance().get(Calendar.MONTH) + 1
                    val season = when(month) {
                        in 3..5 -> "Spring"; in 6..8 -> "Summer"; in 9..11 -> "Autumn"; else -> "Winter"
                    }

                    val rs = OutfitRecommendationService()
                    val bestOutfits = rs.getRecommendations(profile, wardrobe, season, frequencyMap, feedbackMap)
                    if (bestOutfits.isNotEmpty()) {
                        val top = bestOutfits.first()
                        localOutfitContext = "Highly recommended items from user's actual closet: " + 
                            top.items.joinToString { "${it.clothes_name} (${it.category})" }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 3. GỌI GEMINI API ĐỂ XỬ LÝ
            val generativeModel = GenerativeModel(
                modelName = "gemini-3.1-flash-lite",
                apiKey = com.example.dacs3.BuildConfig.GEMINI_API_KEY
            )

            val prompt = """
                You are an expert AI fashion stylist for an app called Wearwise.
                Create a very short, punchy morning outfit notification for the user.
                
                Context:
                - Weather: $weatherCondition, Temperature: $temp.
                - Schedule: $scheduleContext
                - Actual Wardrobe Suggestions (Local RS): $localOutfitContext
                
                Task: Suggest ONE perfect outfit. If local suggestions are provided, PRIORITIZE them as they exist in the user's closet.
                Format exactly like this (no markdown, just 2 lines):
                Line 1 (Title, max 40 chars, include an emoji): <Catchy Title>
                Line 2 (Body, max 100 chars): <Brief outfit suggestion based on weather and schedule>
            """.trimIndent()

            val response = generativeModel.generateContent(prompt)
            val aiText =
                response.text ?: "✨ Good morning! Wear a comfortable T-shirt and jeans today."

            // Bóc tách Title và Body từ phản hồi của Gemini
            val lines = aiText.lines().filter { it.isNotBlank() }
            val title = lines.getOrNull(0) ?: "✨ Good morning! Your outfit is ready."
            val body = lines.getOrNull(1)
                ?: "It's $weatherCondition today. Tap to see your AI-generated outfit!"

            // 4. BẮN THÔNG BÁO CHO NGƯỜI DÙNG
            showOutfitNotification(title, body)

            return@withContext Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            // Nếu mất mạng hoặc API lỗi, bắn thông báo mặc định (Fallback)
            showOutfitNotification(
                "✨ Good morning!",
                "Tap to let AI pick your outfit for the day."
            )
            return@withContext Result.retry()
        }
    }

    // HÀM HỖ TRỢ: ĐỌC LỊCH ĐIỆN THOẠI HÔM NAY
    private fun getTodaysCalendarEvents(context: Context): String {
        // Kiểm tra quyền trước khi đọc lịch
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALENDAR
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return "Calendar permission not granted."
        }

        val projection = arrayOf(CalendarContract.Events.TITLE, CalendarContract.Events.DTSTART)

        // Thiết lập mốc thời gian: Từ 00:00 sáng nay đến 23:59 tối nay
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0)
        val startOfDay = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59)
        val endOfDay = calendar.timeInMillis

        val selection =
            "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?"
        val selectionArgs = arrayOf(startOfDay.toString(), endOfDay.toString())

        val eventsList = mutableListOf<String>()
        val uri: Uri = CalendarContract.Events.CONTENT_URI

        context.contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            "${CalendarContract.Events.DTSTART} ASC"
        )?.use { cursor ->
            val titleIndex = cursor.getColumnIndex(CalendarContract.Events.TITLE)
            while (cursor.moveToNext() && eventsList.size < 3) { // Chỉ lấy tối đa 3 sự kiện để prompt không quá dài
                val title = cursor.getString(titleIndex)
                eventsList.add(title)
            }
        }
        return eventsList.joinToString(", ")
    }

    private fun showOutfitNotification(title: String, body: String) {
        val channelId = "daily_outfit_channel"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Daily Outfit Suggestions",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("wearwise://stylist"),
            context,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle(title) // Dùng Title từ Gemini
            .setContentText(body)   // Dùng Body từ Gemini
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(2003, notification)
    }
}