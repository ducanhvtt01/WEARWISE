package com.example.dacs3.login.be

import com.example.dacs3.connectDB.ErrorParser
import com.example.dacs3.connectDB.supabase
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class LoginResult {
    object SUCCESS : LoginResult()
    object EMAIL_NOT_CONFIRMED : LoginResult()
    object INVALID_CREDENTIALS : LoginResult()
    data class ERROR(val message: String) : LoginResult()
}

public suspend fun logincheck(emailUser: String, passUser: String): LoginResult {
    return withContext(Dispatchers.IO) {
        try {
            // Thực hiện đăng nhập
            supabase.auth.signInWith(Email) {
                email = emailUser
                password = passUser
            }

            // Lấy thông tin user hiện tại
            val user = supabase.auth.currentUserOrNull()

            if (user != null) {
                // KIỂM TRA XÁC NHẬN EMAIL
                // Nếu email_confirmed_at là null, nghĩa là chưa xác nhận
                if (user.emailConfirmedAt == null) {
                    // Đăng xuất ngay lập tức để không giữ session chưa xác thực
                    supabase.auth.signOut()
                    LoginResult.EMAIL_NOT_CONFIRMED
                } else {
                    LoginResult.SUCCESS
                }
            } else {
                LoginResult.INVALID_CREDENTIALS
            }
        } catch (e: Exception) {
            println("Error: ${e.message}")
            val errorMsg = e.message ?: ""
            if (errorMsg.contains("Invalid login credentials", ignoreCase = true) ||
                errorMsg.contains("invalid_credentials", ignoreCase = true)
            ) {
                LoginResult.INVALID_CREDENTIALS
            } else if (errorMsg.contains("Email not confirmed", ignoreCase = true)) {
                LoginResult.EMAIL_NOT_CONFIRMED
            } else {
                LoginResult.ERROR(ErrorParser.parse(e))
            }
        }
    }
}

// --- NEW PASSWORD RESET FUNCTIONS ---

suspend fun sendResetPasswordEmail(email: String): String? {
    return withContext(Dispatchers.IO) {
        try {
            supabase.auth.resetPasswordForEmail(email)
            null
        } catch (e: Exception) {
            e.printStackTrace()
            ErrorParser.parse(e)
        }
    }
}

suspend fun verifyResetOtp(email: String, otp: String): String? {
    return withContext(Dispatchers.IO) {
        try {
            // Dùng OtpType.Email.RECOVERY cho reset password
            supabase.auth.verifyEmailOtp(
                type = io.github.jan.supabase.gotrue.OtpType.Email.RECOVERY,
                email = email,
                token = otp
            )
            null
        } catch (e: Exception) {
            e.printStackTrace()
            ErrorParser.parse(e)
        }
    }
}

suspend fun updateUserPassword(newPassword: String): String? {
    return withContext(Dispatchers.IO) {
        try {
            supabase.auth.updateUser {
                password = newPassword
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            ErrorParser.parse(e)
        }
    }
}