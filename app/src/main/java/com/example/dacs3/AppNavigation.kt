// Chuyên lo điều hướng các màn hình
package com.example.dacs3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.example.dacs3.connectDB.Profile
import com.example.dacs3.connectDB.supabase
import com.example.dacs3.dashboard.StylistScreen
import com.example.dacs3.dashboard.homeui.HomeUI
import com.example.dacs3.login.LoginScreen
import com.example.dacs3.survey.SurveyMasterScreen
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch


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
                    navController.navigate("home") { popUpTo("survey") { inclusive = true } }
                }
            )
        }

        composable("home") {
            RequestWeatherPermissions()
            HomeUI(
                isDarkMode = isDarkMode,
                onThemeChange = onThemeChange,
                onLogoutSuccess = {
                    navController.navigate("login") { popUpTo("home") { inclusive = true } }
                }
            )
        }

        composable(
            route = "stylist",
            deepLinks = listOf(navDeepLink { uriPattern = "wearwise://stylist" })
        ) {
            StylistScreen()
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
        navController.navigate(destination) { popUpTo("login") { inclusive = true } }
    } catch (e: Exception) {
        navController.navigate("survey") { popUpTo("login") { inclusive = true } }
    }
}