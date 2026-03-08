
package com.example.dacs3.connectDB

import android.content.Context
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth

object SupabaseManager {
    lateinit var client: io.github.jan.supabase.SupabaseClient
        private set

    fun init(context: Context) {
        client = createSupabaseClient(
            supabaseUrl = "https://lhtytvfpuwbcghesagcm.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxodHl0dmZwdXdiY2doZXNhZ2NtIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzI0MzI5NTIsImV4cCI6MjA4ODAwODk1Mn0.jxOTqxZhshHfY_2ldBWVoC6sJB1T1-lmVeMbJoxCuMk"
        ) {
            install(Auth) {
                // TRUYỀN CONTEXT VÀO ĐÂY
                sessionManager = SettingsSessionManager(context.applicationContext)
                alwaysAutoRefresh = true
            }
        }
    }
}

// Shortcut để các file khác vẫn gọi được 'supabase' như cũ
val supabase get() = SupabaseManager.client