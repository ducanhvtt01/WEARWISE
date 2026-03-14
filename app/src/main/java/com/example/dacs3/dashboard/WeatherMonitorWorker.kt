package com.example.dacs3.dashboard

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class WeatherMonitorWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // 1. Chuyển đổi Callback của LocationHelper thành Coroutine để Worker chờ lấy tọa độ
            val location = suspendCancellableCoroutine<Pair<Double, Double>?> { continuation ->
                LocationHelper.getCurrentLocation(
                    context = applicationContext,
                    onLocationFetched = { lat, lon ->
                        continuation.resume(Pair(lat, lon)) // Trả về tọa độ khi thành công
                    },
                    onFailed = {
                        continuation.resume(null) // Trả về null khi thất bại
                    }
                )
            }

            // Nếu không lấy được vị trí GPS (do tắt vị trí, mất sóng GPS...), báo Worker thử lại sau
            if (location == null) {
                return@withContext Result.retry()
            }

            val (lat, lon) = location
            val apiKey = "a2ae8f3c11d5bff557f2e81f52a543db"

            // 2. Gọi API bằng Kinh độ (lon) và Vĩ độ (lat)
            val response = RetrofitInstance.api.getWeatherByLocation(lat, lon, apiKey)

            val condition = response.weather.firstOrNull()?.main ?: "Clear"

            // Đa số API thời tiết (như OpenWeatherMap) sẽ trả về tên khu vực dựa trên tọa độ ở trường 'name'
            val locationName = response.name ?: "your area"

            // 3. Kích hoạt thông báo nếu có mưa
            if (condition.equals("Rain", ignoreCase = true) ||
                condition.equals("Thunderstorm", ignoreCase = true) ||
                condition.equals("Drizzle", ignoreCase = true)
            ) {
                showEmergencyNotification(locationName)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun showEmergencyNotification(locationName: String) {
        val channelId = "weather_alert_channel"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Weather Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // Bạn có thể đổi icon cái ô/mưa ở đây
            .setContentTitle("☔ Rain Alert in $locationName!")
            .setContentText("It looks like rain is coming to $locationName. Don't forget your raincoat or umbrella before heading out!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("It looks like rain is coming to $locationName. Don't forget your raincoat or umbrella before heading out!")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(2002, notification)
    }
}