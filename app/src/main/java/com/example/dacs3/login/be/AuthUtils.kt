package com.example.dacs3.login.be

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.dacs3.connectDB.supabase
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.OAuthProvider

var authErrorMessage by mutableStateOf<String?>(null)

suspend fun signInWithSocial(provider: OAuthProvider): Boolean {
    return try {
        authErrorMessage = null

        supabase.auth.signInWith(
            provider = provider,
            redirectUrl = "my-app-scheme://auth-callback"
        )

        true
    } catch (e: Exception) {
        Log.e("AuthError", "Social login failed: ${e.message}", e)
        authErrorMessage = "Error: ${e.localizedMessage ?: "Cannot connect to server"}"
        false
    }
}