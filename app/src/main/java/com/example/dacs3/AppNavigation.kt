package com.example.dacs3

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.example.dacs3.connectDB.DashboardViewModel
import com.example.dacs3.connectDB.Profile
import com.example.dacs3.connectDB.supabase
import com.example.dacs3.dashboard.CalendarScreen
import com.example.dacs3.dashboard.LaundryScreen
import com.example.dacs3.dashboard.TodoScreen
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
    val dashboardViewModel: DashboardViewModel = viewModel()

    LaunchedEffect(Unit) {
        val session = supabase.auth.currentSessionOrNull()
        if (session != null) {
            checkProfileAndNavigate(session.user?.id, navController)
        }
    }

    NavHost(
        navController = navController,
        startDestination = "login",
        enterTransition = { androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(200)) },
        exitTransition = { androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(200)) },
        popEnterTransition = { androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(200)) },
        popExitTransition = { androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(200)) }
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
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
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
                    ExitTransition.None
                } else {
                    null
                }
            }
        ) {
            RequestAppPermissions() // Lưu ý: Đảm bảo bạn đã có hàm này ở đâu đó trong project

            HomeUI(
                isDarkMode = isDarkMode,
                onThemeChange = onThemeChange,
                viewModel = dashboardViewModel,
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
                },
                onNavigateToTodo = {
                    navController.navigate("todo")
                },
                onNavigateToCalendar = {
                    navController.navigate("calendar")
                },
                onNavigateToLaundry = {
                    navController.navigate("laundry")
                }
            )
        }

        composable("todo") {
            TodoScreen(
                viewModel = dashboardViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("calendar") {
            CalendarScreen(
                viewModel = dashboardViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("laundry") {
            LaundryScreen(
                viewModel = dashboardViewModel,
                onBack = { navController.popBackStack() }
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
            // Chú ý: Ở đây gọi trực tiếp StylistScreen() không truyền ViewModel,
            // đảm bảo bên trong file StylistScreen.kt bạn đã khai báo viewModel() làm mặc định.
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