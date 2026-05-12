package com.example.dacs3

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import io.github.jan.supabase.gotrue.handleDeeplinks
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var currentIntent by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Khởi tạo Supabase trước
        SupabaseManager.init(this)

        // Xử lý deep link OAuth nếu app được mở từ Google login
        try {
            supabase.handleDeeplinks(intent)
            Log.d("OAuthDebug", "handleDeeplinks called in onCreate")
        } catch (e: Exception) {
            Log.e("OAuthDebug", "handleDeeplinks onCreate error: ${e.message}", e)
        }

        enableEdgeToEdge()

        WorkerSetupHelper.setupWeatherMonitor(this)
        WorkerSetupHelper.setupDailyOutfitNotification(this)

        currentIntent = intent

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            val context = LocalContext.current
            val themeManager = remember { ThemeManager(context) }
            val scope = rememberCoroutineScope()
            val isDarkMode by themeManager.themeFlow.collectAsState(initial = false)
            val navController = rememberNavController()

            LaunchedEffect(currentIntent) {
                val intentData = currentIntent?.data

                Log.d("OAuthDebug", "LaunchedEffect currentIntent = $currentIntent")
                Log.d("OAuthDebug", "Intent data = $intentData")

                if (intentData == null) {
                    return@LaunchedEffect
                }

                Log.d("OAuthDebug", "Deep link received: $intentData")
                Log.d("OAuthDebug", "Scheme: ${intentData.scheme}, Host: ${intentData.host}")

                val isSupabaseAuthCallback =
                    intentData.scheme == "my-app-scheme" &&
                            intentData.host == "auth-callback"

                if (!isSupabaseAuthCallback) {
                    Log.d("OAuthDebug", "Not Supabase auth callback, ignored.")
                    return@LaunchedEffect
                }

                var userId: String? = null

                for (i in 1..20) {
                    val session = supabase.auth.currentSessionOrNull()
                    val user = supabase.auth.currentUserOrNull()

                    Log.d(
                        "OAuthDebug",
                        "Check user attempt $i | session=${session != null} | userId=${user?.id}"
                    )

                    if (user != null) {
                        userId = user.id
                        break
                    }

                    delay(250)
                }

                if (userId != null) {
                    Log.d("OAuthDebug", "OAuth login success. Navigate to survey. userId=$userId")

                    navController.navigate("survey") {
                        popUpTo("login") {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                } else {
                    Log.e(
                        "OAuthDebug",
                        "Deep link received but Supabase user is still null after waiting."
                    )
                }
            }

            WearwiseTheme(darkTheme = isDarkMode) {
                AppNavigation(
                    navController = navController,
                    isDarkMode = isDarkMode,
                    onThemeChange = { isDark ->
                        scope.launch {
                            themeManager.saveTheme(isDark)
                        }
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        Log.d("OAuthDebug", "onNewIntent called with data: ${intent.data}")

        try {
            supabase.handleDeeplinks(intent)
            Log.d("OAuthDebug", "handleDeeplinks called in onNewIntent")
        } catch (e: Exception) {
            Log.e("OAuthDebug", "handleDeeplinks onNewIntent error: ${e.message}", e)
        }

        currentIntent = intent
    }
}