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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

// --- IMPORT CÁC MÀN HÌNH ---
import com.example.dacs3.dashboard.HomeUI
import com.example.dacs3.login.LoginScreen
import com.example.dacs3.survey.Step1Screen // <-- Đã thêm Import này
import com.example.dacs3.survey.Step2Screen
import com.example.dacs3.survey.Step3Screen
import com.example.dacs3.survey.SurveyViewModel
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
    val surveyViewModel: SurveyViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "login",
        // --- HIỆU ỨNG SLIDE POWERPOINT ---
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(500)
            ) + fadeIn(animationSpec = tween(500))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth },
                animationSpec = tween(500)
            ) + fadeOut(animationSpec = tween(500))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth },
                animationSpec = tween(500)
            ) + fadeIn(animationSpec = tween(500))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(500)
            ) + fadeOut(animationSpec = tween(500))
        }
    ) {
        // --- ĐĂNG NHẬP ---
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("survey_step_1") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // --- KHẢO SÁT ---
        composable("survey_step_1") {
            Step1Screen(
                viewModel = surveyViewModel,
                onNext = { navController.navigate("survey_step_2") }
            )
        }

        composable("survey_step_2") {
            Step2Screen(
                viewModel = surveyViewModel,
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate("survey_step_3") }
            )
        }

        composable("survey_step_3") {
            Step3Screen(
                viewModel = surveyViewModel,
                onBack = { navController.popBackStack() },
                onComplete = {
                    // Xử lý gửi dữ liệu rồi về Home
                    navController.navigate("home") {
                        popUpTo("survey_step_1") { inclusive = true }
                    }
                }
            )
        }

        // --- TRANG CHỦ ---
        composable("home") {
            HomeUI()
        }
    }
}