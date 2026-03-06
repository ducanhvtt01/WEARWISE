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
            println("Lỗi: ${e.message}")
            if (e.message?.contains("Invalid login credentials", ignoreCase = true) == true) {
                LoginResult.INVALID_CREDENTIALS
            } else {
                LoginResult.ERROR
            }
        }
    }
}

/**
 * Hàm kiểm tra xem người dùng có đang đăng nhập hay không.
 * Trả về true nếu có session hợp lệ, ngược lại trả về false.
 */
public suspend fun isUserLoggedIn(): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            // Kiểm tra xem currentSession có tồn tại hay không
            val session = supabase.auth.currentSessionOrNull()
            session != null
        } catch (e: Exception) {
            false
        }
    }
}