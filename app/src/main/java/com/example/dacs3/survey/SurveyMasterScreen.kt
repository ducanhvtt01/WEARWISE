package com.example.dacs3.survey

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.example.dacs3.connectDB.supabase
import io.github.jan.supabase.gotrue.auth

//File này để gọi SurveyLayout cố định và NavHost để làm hiệu ứng trượt
@Composable
fun SurveyMasterScreen(onFinish: () -> Unit) {
    val surveyViewModel: SurveyViewModel = viewModel()
    val childNavController = rememberNavController()
    val userId = remember { supabase.auth.currentUserOrNull()?.id ?: ""}

    val navBackStackEntry by childNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentStep = when (currentRoute) {
        "step2" -> 2
        "step3" -> 3
        else -> 1
    }

    LaunchedEffect(surveyViewModel.updateSuccess) {
        if (surveyViewModel.updateSuccess) {
            onFinish()
        }
    }

    SurveyLayout(
        currentStep = currentStep,
        onBack = if (currentStep > 1) { { childNavController.popBackStack() } } else null,
        onNext = {
            when (currentStep) {
                1 -> childNavController.navigate("step2")
                2 -> childNavController.navigate("step3")
                3 -> {
                    userId.let { surveyViewModel.updateProfile() }
                }
            }
        }
    ) {
        NavHost(
            navController = childNavController,
            startDestination = "step1",
            enterTransition = { slideInHorizontally(tween(500)) { it } + fadeIn() },
            exitTransition = { slideOutHorizontally(tween(500)) { -it } + fadeOut() },
            popEnterTransition = { slideInHorizontally(tween(500)) { -it } + fadeIn() },
            popExitTransition = { slideOutHorizontally(tween(500)) { it } + fadeOut() }
        ) {
            composable("step1") { Step1Screen(surveyViewModel) }
            composable("step2") { Step2Screen(surveyViewModel) }
            composable("step3") { Step3Screen(surveyViewModel) }
        }
    }
}