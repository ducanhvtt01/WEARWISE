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
import androidx.navigation.compose.rememberNavController
import com.example.dacs3.connectDB.SupabaseManager
import com.example.dacs3.connectDB.supabase
import com.example.dacs3.ui.theme.ThemeManager
import com.example.dacs3.ui.theme.WearwiseTheme
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.parseSessionFromUrl
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var currentIntent by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SupabaseManager.init(this)
        enableEdgeToEdge()

        // Gọi 2 Worker từ file Helper
        WorkerSetupHelper.setupWeatherMonitor(this)
        WorkerSetupHelper.setupDailyOutfitNotification(this)

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

            LaunchedEffect(currentIntent) {
                currentIntent?.data?.let { uri ->
                    try {
                        val uriString = uri.toString()
                        if (uriString.contains("auth-callback") || uriString.contains("survey")) {
                            supabase.auth.parseSessionFromUrl(uriString)
                            delay(300)
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
                // Gọi UI Điều hướng từ file AppNavigation
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        currentIntent = intent
    }
}