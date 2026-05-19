package com.example.dacs3.login.be

import com.example.dacs3.connectDB.ErrorParser
import com.example.dacs3.connectDB.supabase
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

public suspend fun onRegister(email: String, pass: String, name: String): String? {
    return try {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = pass
            data = buildJsonObject {
                put("full_name", name)
            }
        }
        println("Register successfully, please check your email to confirm!")
        null
    } catch (e: Exception) {
        e.printStackTrace()
        ErrorParser.parse(e)
    }
}