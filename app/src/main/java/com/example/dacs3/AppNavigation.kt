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
import com.example.dacs3.shop.SeasonalStoresMapScreen

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
                showSplash = true,
                onLoginSuccess = {
                    val session = supabase.auth.currentSessionOrNull()
                    if (session != null) {
                        scope.launch {
                            checkProfileAndNavigate(session.user?.id, navController)
                        }
                    }
                }
            )
        }

        composable(
            route = "login_no_splash",
            enterTransition = { androidx.compose.animation.EnterTransition.None },
            exitTransition = { androidx.compose.animation.ExitTransition.None },
            popEnterTransition = { androidx.compose.animation.EnterTransition.None },
            popExitTransition = { androidx.compose.animation.ExitTransition.None }
        ) {
            LoginScreen(
                showSplash = false,
                onLoginSuccess = {
                    val session = supabase.auth.currentSessionOrNull()
                    if (session != null) {
                        scope.launch {
                            checkProfileAndNavigate(
                                userId = session.user?.id,
                                navController = navController,
                                popUpRoute = "login_no_splash"
                            )
                        }
                    }
                }
            )
        }

        composable(route = "survey") {
            SurveyMasterScreen(
                onFinish = {
                    navController.navigate("home") {
                        popUpTo("survey") {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = "home",
            exitTransition = {
                if (targetState.destination.route == "login_no_splash") {
                    androidx.compose.animation.ExitTransition.None
                } else {
                    null
                }
            }
        ) {
            RequestAppPermissions()

            HomeUI(
                isDarkMode = isDarkMode,
                onThemeChange = onThemeChange,
                onLogoutSuccess = {
                    navController.navigate("login_no_splash") {
                        popUpTo("home") {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                onOpenSeasonStores = { season ->
                    navController.navigate("season_stores/$season")
                }
            )
        }
        composable("season_stores/{season}") { backStackEntry ->
            val season = backStackEntry.arguments?.getString("season") ?: "autumn"

            SeasonalStoresMapScreen(
                season = season,
                onBack = {
                    navController.popBackStack()
                }
            )
        }


        composable(
            route = "stylist",
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "wearwise://stylist"
                }
            )
        ) {
            StylistScreen()
        }
    }
}

suspend fun checkProfileAndNavigate(
    userId: String?,
    navController: NavHostController,
    popUpRoute: String = "login"
) {
    if (userId == null) return

    try {
        val profile = supabase.from("profiles")
            .select {
                filter {
                    eq("id", userId)
                }
            }
            .decodeSingleOrNull<Profile>()

        val isSurveyCompleted =
            profile != null &&
                    !profile.gender.isNullOrBlank()

        val destination = if (isSurveyCompleted) {
            "home"
        } else {
            "survey"
        }

        navController.navigate(destination) {
            popUpTo(popUpRoute) {
                inclusive = true
            }
            launchSingleTop = true
        }
    } catch (e: Exception) {
        navController.navigate("survey") {
            popUpTo(popUpRoute) {
                inclusive = true
            }
            launchSingleTop = true
        }
    }
}