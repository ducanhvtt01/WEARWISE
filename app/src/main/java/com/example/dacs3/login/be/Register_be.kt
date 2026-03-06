package com.example.dacs3.login.be

import com.example.dacs3.connectDB.supabase
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email


public suspend fun onRegister(email: String, pass: String): Unit {
    try {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = pass
        }
        println("Register successfully, please check your email to confirm!")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}