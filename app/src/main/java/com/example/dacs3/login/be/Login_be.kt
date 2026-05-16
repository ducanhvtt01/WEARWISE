package com.example.dacs3.login.be

import com.example.dacs3.connectDB.supabase
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class LoginResult {
    SUCCESS,
    EMAIL_NOT_CONFIRMED,
    INVALID_CREDENTIALS,
    ERROR
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
            if (e.message?.contains("Invalid login credentials", ignoreCase = true) == true) {
                LoginResult.INVALID_CREDENTIALS
            } else {
                LoginResult.ERROR
            }
        }
    }
}

// --- NEW PASSWORD RESET FUNCTIONS ---

suspend fun sendResetPasswordEmail(email: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            supabase.auth.resetPasswordForEmail(email)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

suspend fun verifyResetOtp(email: String, otp: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            // Dùng OtpType.Email.RECOVERY cho reset password
            supabase.auth.verifyEmailOtp(
                type = io.github.jan.supabase.gotrue.OtpType.Email.RECOVERY,
                email = email,
                token = otp
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

suspend fun updateUserPassword(newPassword: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            supabase.auth.updateUser {
                password = newPassword
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}