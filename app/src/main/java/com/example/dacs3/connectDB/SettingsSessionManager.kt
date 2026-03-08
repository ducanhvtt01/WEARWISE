package com.example.dacs3.connectDB

import android.content.Context
import io.github.jan.supabase.gotrue.SessionManager
import io.github.jan.supabase.gotrue.user.UserSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

public class SettingsSessionManager(context: Context) : SessionManager {
    // Khởi tạo SharedPreferences
    private val sharedPreferences = context.getSharedPreferences("supabase_prefs", Context.MODE_PRIVATE)

    override suspend fun saveSession(session: UserSession) {
        // Chuyển đối tượng session thành chuỗi JSON và lưu vào máy
        val sessionJson = Json.encodeToString(session)
        sharedPreferences.edit().putString("current_session", sessionJson).apply()
    }

    override suspend fun loadSession(): UserSession? {
        // Đọc chuỗi JSON từ máy lên
        val sessionJson = sharedPreferences.getString("current_session", null)
        return try {
            // Chuyển ngược từ JSON về đối tượng UserSession
            sessionJson?.let { Json.decodeFromString<UserSession>(it) }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun deleteSession() {
        // Xóa sạch khi người dùng Logout
        sharedPreferences.edit().remove("current_session").apply()
    }
}