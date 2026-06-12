package com.example.dacs3.connectDB

import android.content.Context
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

object SupabaseManager {
    lateinit var client: io.github.jan.supabase.SupabaseClient
        private set

    fun init(context: Context) {
        client = createSupabaseClient(
            supabaseUrl = com.example.dacs3.BuildConfig.SUPABASE_URL,
            supabaseKey = com.example.dacs3.BuildConfig.SUPABASE_KEY
        ) {
            install(Auth) {

                sessionManager = SettingsSessionManager(context.applicationContext)
                alwaysAutoRefresh = true

                // Quan trọng cho OAuth deep link
                scheme = "my-app-scheme"
                host = "auth-callback"
            }

            install(Postgrest)
            install(Storage)
            install(Realtime)
        }
    }
}

val supabase get() = SupabaseManager.client