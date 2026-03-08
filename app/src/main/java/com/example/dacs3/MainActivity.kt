package com.example.dacs3

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.example.dacs3.connectDB.Profile
import com.example.dacs3.connectDB.SupabaseManager
import com.example.dacs3.connectDB.supabase
import com.example.dacs3.dashboard.HomeUI
import com.example.dacs3.login.LoginScreen
import com.example.dacs3.survey.SurveyMasterScreen
import com.example.dacs3.ui.theme.ThemeManager
import com.example.dacs3.ui.theme.WearwiseTheme
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.parseSessionFromUrl
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // Tạo một state để theo dõi intent mới nhất nhận được
    private var currentIntent by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SupabaseManager.init(this)
        enableEdgeToEdge()

        // Cập nhật intent lần đầu khi App mở
        currentIntent = intent

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            val context = LocalContext.current
            val themeManager = remember { ThemeManager(context) }
            val scope = rememberCoroutineScope()
            val isDarkMode by themeManager.themeFlow.collectAsState(initial = false)

            val navController = rememberNavController()

            // Xử lý Deep Link mỗi khi currentIntent thay đổi
            LaunchedEffect(currentIntent) {
                currentIntent?.data?.let { uri ->
                    try {
                        // 1. Lưu session Supabase
                        supabase.auth.parseSessionFromUrl(uri.toString())

                        // 2. Chờ một chút để NavGraph sẵn sàng
                        delay(300)

                        // 3. Ép điều hướng nếu là link callback hoặc link survey
                        val uriString = uri.toString()
                        if (uriString.contains("auth-callback") || uriString.contains("survey")) {
                            navController.navigate("survey") {
                                popUpTo("login") { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            WearwiseTheme(darkTheme = isDarkMode) {
                AppNavigation(
                    navController = navController,
                    isDarkMode = isDarkMode,
                    onThemeChange = { isDark ->
                        scope.launch { themeManager.saveTheme(isDark) }
                    }
                )
            }
        }
    }

    // QUAN TRỌNG: Cập nhật intent khi nhấn link lúc App đang chạy ngầm
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        currentIntent = intent
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    isDarkMode: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        val session = supabase.auth.currentSessionOrNull()
        if (session != null) {
            checkProfileAndNavigate(session.user?.id, navController)
        }
    }
    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    val session1 = supabase.auth.currentSessionOrNull()
                    if (session1 != null) scope.launch {
                        checkProfileAndNavigate(session1.user?.id, navController)
                    }
                }
            )
        }

        composable(
            route = "survey",
            deepLinks = listOf(
                navDeepLink { uriPattern = "io.supabase.user://survey" },
                navDeepLink { uriPattern = "my-app-scheme://auth-callback" }
            )
        ) {
            SurveyMasterScreen(
                onFinish = {
                    navController.navigate("home") {
                        popUpTo("survey") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeUI(isDarkMode = isDarkMode, onThemeChange = onThemeChange)
        }
    }
}

suspend fun checkProfileAndNavigate(userId: String?, navController: NavHostController) {
    if (userId == null) return
    try {
        val profile = supabase.from("profiles").select {
            filter { eq("id", userId) }
        }.decodeSingleOrNull<Profile>()

        val destination = if (profile != null && !profile.gender.isNullOrBlank()) "home" else "survey"

        navController.navigate(destination) {
            popUpTo("login") { inclusive = true }
        }
    } catch (e: Exception) {
        navController.navigate("survey") {
            popUpTo("login") { inclusive = true }
        }
    }
}