package com.example.dacs3.ui.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Khởi tạo file bộ nhớ tên là "settings"
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class ThemeManager(private val context: Context) {
    companion object {
        // Tạo một cái "Chìa khóa" để lưu trạng thái
        val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
    }

    // Đọc trạng thái từ bộ nhớ (Mặc định là false - Sáng)
    val themeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DARK_MODE_KEY] ?: false
    }

    // Ghi trạng thái mới vào bộ nhớ
    suspend fun saveTheme(isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = isDark
        }
    }
}