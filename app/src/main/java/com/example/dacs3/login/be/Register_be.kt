package com.example.dacs3.login.be

import com.example.dacs3.connectDB.Profile
import com.example.dacs3.connectDB.supabase
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
public suspend fun onRegister(email: String, pass: String, name: String): Unit {
    try {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = pass
            data = buildJsonObject {
                put("full_name", name)
            }
        }

        println("Register successfully, please check your email to confirm!")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}