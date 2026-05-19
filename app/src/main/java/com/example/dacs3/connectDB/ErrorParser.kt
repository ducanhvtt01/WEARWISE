package com.example.dacs3.connectDB

object ErrorParser {
    fun parse(throwable: Throwable): String {
        val className = throwable::class.qualifiedName ?: throwable::class.simpleName ?: ""
        val message = throwable.message ?: ""
        
        // Ghi log lỗi để debug
        println("Parsing error: Class=$className, Message=$message")
        throwable.printStackTrace()

        // 1. Kiểm tra lỗi mất kết nối mạng hoặc timeout
        if (className.contains("UnknownHostException", ignoreCase = true) ||
            className.contains("ConnectException", ignoreCase = true) ||
            className.contains("SocketTimeoutException", ignoreCase = true) ||
            className.contains("Timeout", ignoreCase = true) ||
            message.contains("Unable to resolve host", ignoreCase = true) ||
            message.contains("ConnectException", ignoreCase = true) ||
            message.contains("Failed to connect", ignoreCase = true) ||
            message.contains("timeout", ignoreCase = true)
        ) {
            return "Cannot connect to the server. Please check your Wifi or mobile data connection."
        }

        // 2. Phân tích các thông điệp lỗi trả về từ Supabase (thường nằm trong message)
        return when {
            // Lỗi thông tin đăng nhập không hợp lệ
            message.contains("Invalid login credentials", ignoreCase = true) ||
            message.contains("invalid_credentials", ignoreCase = true) ->
                "Incorrect email or password. Please try again."

            // Lỗi email chưa được xác nhận
            message.contains("Email not confirmed", ignoreCase = true) ||
            message.contains("email_not_confirmed", ignoreCase = true) ->
                "This email address is not verified. Please check your inbox to confirm."

            // Lỗi đăng ký trùng email
            message.contains("User already registered", ignoreCase = true) ||
            message.contains("user_already_exists", ignoreCase = true) ||
            message.contains("already exists", ignoreCase = true) ->
                "This email address is already registered by another account."

            // Lỗi gửi OTP quá nhanh (rate limit)
            message.contains("rate limit", ignoreCase = true) ||
            message.contains("too many requests", ignoreCase = true) ||
            message.contains("429", ignoreCase = true) ||
            message.contains("resend", ignoreCase = true) && message.contains("limit", ignoreCase = true) ->
                "Action too frequent. Please wait a few minutes and try again."

            // Lỗi mã OTP không hợp lệ hoặc hết hạn
            message.contains("otp", ignoreCase = true) && (message.contains("invalid", ignoreCase = true) || message.contains("expired", ignoreCase = true)) ||
            message.contains("token", ignoreCase = true) && (message.contains("invalid", ignoreCase = true) || message.contains("expired", ignoreCase = true)) ||
            message.contains("verify", ignoreCase = true) && message.contains("fail", ignoreCase = true) ||
            message.contains("invalid flow state", ignoreCase = true) ->
                "Invalid or expired verification code. Please check and try again."

            // Lỗi mật khẩu không đủ mạnh hoặc quá ngắn
            message.contains("Password should be", ignoreCase = true) || 
            message.contains("password must be", ignoreCase = true) ||
            message.contains("weak password", ignoreCase = true) ->
                "Password is too weak or does not meet the minimum length (at least 6 characters)."

            // Lỗi email không hợp lệ
            message.contains("invalid email", ignoreCase = true) || 
            message.contains("email address is invalid", ignoreCase = true) ||
            message.contains("validation_failed", ignoreCase = true) && message.contains("email", ignoreCase = true) ->
                "Invalid email address format."

            // Lỗi không tìm thấy người dùng (khi reset password)
            message.contains("User not found", ignoreCase = true) ->
                "No user account found with the entered email."

            // Các lỗi phát sinh từ Postgrest (database)
            message.contains("violates unique constraint", ignoreCase = true) ->
                "Duplicate data found in the system."
            
            message.contains("violates foreign key", ignoreCase = true) ->
                "Invalid data reference."

            // Thông báo lỗi mặc định nếu có message
            message.isNotBlank() && !message.contains("Exception", ignoreCase = true) ->
                "Error: $message"

            else -> "A system error occurred. Please try again later."
        }
    }
}
