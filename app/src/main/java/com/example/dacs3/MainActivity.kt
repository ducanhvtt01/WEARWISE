package com.example.dacs3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dacs3.dashboard.HomeUI
import com.example.dacs3.login.LoginScreen // Đổi tên từ LoginUI sang LoginScreen
import com.example.dacs3.ui.theme.DACS3Theme // Kiểm tra lại import theme của bạn

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Kích hoạt vẽ tràn viền
        enableEdgeToEdge()

        // --- BẮT ĐẦU ĐOẠN CODE ẨN THANH ĐIỀU HƯỚNG VÀ TRẠNG THÁI ---
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

        // Cài đặt hành vi: Khi người dùng vuốt từ mép trên hoặc mép dưới màn hình,
        // các thanh này sẽ hiện ra tạm thời trong vài giây rồi tự ẩn đi lại.
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Thực thi lệnh ẩn TẤT CẢ các thanh hệ thống (system bars)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        // --- KẾT THÚC ĐOẠN CODE ---

        setContent {
            DACS3Theme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // Khởi chạy vào màn hình login đầu tiên
    NavHost(navController = navController, startDestination = "login") {

        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    // Khi đăng nhập/đăng ký thành công, đi đến "home" và xóa màn "login" khỏi lịch sử
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeUI()
        }
    }
}