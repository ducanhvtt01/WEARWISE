package com.example.dacs3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

// --- IMPORT CÁC MÀN HÌNH ---
import com.example.dacs3.dashboard.HomeUI
import com.example.dacs3.login.LoginScreen
import com.example.dacs3.survey.SurveyMasterScreen // <-- Gọi màn hình điều phối khảo sát mới
import com.example.dacs3.ui.theme.DACS3Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

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

    NavHost(
        navController = navController,
        startDestination = "login",
        // Hiệu ứng chuyển cảnh giữa các màn hình chính (Login -> Survey -> Home)
        enterTransition = {
            slideInHorizontally(animationSpec = tween(500)) { it } + fadeIn(tween(500))
        },
        exitTransition = {
            slideOutHorizontally(animationSpec = tween(500)) { -it } + fadeOut(tween(500))
        },
        popEnterTransition = {
            slideInHorizontally(animationSpec = tween(500)) { -it } + fadeIn(tween(500))
        },
        popExitTransition = {
            slideOutHorizontally(animationSpec = tween(500)) { it } + fadeOut(tween(500))
        }
    ) {
        // --- 1. MÀN HÌNH ĐĂNG NHẬP ---
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    // Chuyển thẳng vào luồng khảo sát
                    navController.navigate("survey") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // --- 2. LUỒNG KHẢO SÁT (MASTER) ---
        // Tại đây, SurveyMasterScreen sẽ tự quản lý việc trượt giữa 3 bước bên trong nó
        composable("survey") {
            SurveyMasterScreen(
                onFinish = {
                    navController.navigate("home") {
                        popUpTo("survey") { inclusive = true }
                    }
                }
            )
        }

        // --- 3. TRANG CHỦ ---
        composable("home") {
            HomeUI()
        }
    }
}