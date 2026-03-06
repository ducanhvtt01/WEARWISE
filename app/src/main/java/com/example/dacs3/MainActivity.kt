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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

// --- IMPORT CÁC MÀN HÌNH ---
import com.example.dacs3.dashboard.HomeUI
import com.example.dacs3.login.LoginScreen
import com.example.dacs3.survey.SurveyMasterScreen
import com.example.dacs3.ui.theme.ThemeManager
import com.example.dacs3.ui.theme.WearwiseTheme // Hãy đảm bảo bạn đã import đúng đường dẫn Theme của bạn
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            // 1. Lấy context và khởi tạo ThemeManager
            val context = LocalContext.current
            val themeManager = remember { ThemeManager(context) }
            val scope = rememberCoroutineScope()

            // 2. Đọc trạng thái lưu trong DataStore (app khởi động sẽ tự lấy giá trị cũ)
            val isDarkMode by themeManager.themeFlow.collectAsState(initial = false)

            // 3. Áp dụng Theme
            WearwiseTheme(darkTheme = isDarkMode) {
                AppNavigation(
                    isDarkMode = isDarkMode,
                    onThemeChange = { isDark ->
                        // Khi gạt nút trong Profile, lưu ngay trạng thái vào DataStore
                        scope.launch {
                            themeManager.saveTheme(isDark)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AppNavigation(isDarkMode: Boolean, onThemeChange: (Boolean) -> Unit) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login",
        enterTransition = { slideInHorizontally(animationSpec = tween(500)) { it } + fadeIn(tween(500)) },
        exitTransition = { slideOutHorizontally(animationSpec = tween(500)) { -it } + fadeOut(tween(500)) },
        popEnterTransition = { slideInHorizontally(animationSpec = tween(500)) { -it } + fadeIn(tween(500)) },
        popExitTransition = { slideOutHorizontally(animationSpec = tween(500)) { it } + fadeOut(tween(500)) }
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("survey") { popUpTo("login") { inclusive = true } }
                }
            )
        }

        composable("survey") {
            SurveyMasterScreen(
                onFinish = {
                    navController.navigate("home") { popUpTo("survey") { inclusive = true } }
                }
            )
        }

        composable("home") {
            // Truyền trạng thái theme xuống cho HomeUI
            HomeUI(isDarkMode = isDarkMode, onThemeChange = onThemeChange)
        }
    }
}