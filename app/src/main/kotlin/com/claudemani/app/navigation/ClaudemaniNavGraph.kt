package com.claudemani.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.claudemani.app.presentation.AppViewModel
import com.claudemani.app.ui.screens.addword.AddWordScreen
import com.claudemani.app.ui.screens.about.AboutScreen
import com.claudemani.app.ui.screens.categories.CategoriesScreen
import com.claudemani.app.ui.screens.home.HomeScreen
import com.claudemani.app.ui.screens.progress.ProgressScreen
import com.claudemani.app.ui.screens.review.ReviewScreen
import com.claudemani.app.ui.screens.settings.SettingsScreen
import com.claudemani.domain.model.AppTheme

/**
 * Wires [Routes] to their screens (Descriptions §13/Phase 8). Navigation
 * is isolated here in the app module and never leaks into domain/data
 * (§3.3) — this file and [AppViewModel] are the only places that know
 * about route strings at all.
 *
 * Each `composable(...)` block reports its own route to [AppViewModel]
 * via [LaunchedEffect] on entering composition, rather than the
 * NavGraph tracking selection itself — matches "AppViewModel handles
 * route selection only".
 *
 * [currentTheme]/[onThemeChange] (Phase 28) are plain pass-through
 * parameters from the Activity-scoped `ThemeViewModel` in `MainActivity`
 * — see that class's KDoc for why Settings does not read its own,
 * separate copy of the current theme.
 */
@Composable
fun ClaudemaniNavGraph(
    navController: NavHostController = rememberNavController(),
    appViewModel: AppViewModel = hiltViewModel(),
    currentTheme: AppTheme = AppTheme.SYSTEM,
    onThemeChange: (AppTheme) -> Unit = {}
) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            LaunchedEffect(Unit) { appViewModel.onRouteChanged(Routes.HOME) }
            HomeScreen(
                onStartReview = { reviewType -> navController.navigate(Routes.reviewRoute(reviewType)) },
                onNavigateToAddWord = { navController.navigate(Routes.ADD_WORD) },
                onNavigateToProgress = { navController.navigate(Routes.PROGRESS) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(
            route = Routes.REVIEW_ROUTE_PATTERN,
            arguments = listOf(
                navArgument(Routes.REVIEW_TYPE_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            LaunchedEffect(Unit) { appViewModel.onRouteChanged(Routes.REVIEW) }
            val reviewType = backStackEntry.arguments?.getString(Routes.REVIEW_TYPE_ARG)
            ReviewScreen(reviewTypeArg = reviewType, onFinished = { navController.popBackStack() })
        }
        composable(Routes.ADD_WORD) {
            LaunchedEffect(Unit) { appViewModel.onRouteChanged(Routes.ADD_WORD) }
            AddWordScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.PROGRESS) {
            LaunchedEffect(Unit) { appViewModel.onRouteChanged(Routes.PROGRESS) }
            ProgressScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            LaunchedEffect(Unit) { appViewModel.onRouteChanged(Routes.SETTINGS) }
            SettingsScreen(
                currentTheme = currentTheme,
                onThemeChange = onThemeChange,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCategories = { navController.navigate(Routes.CATEGORIES) },
                onNavigateToAbout = { navController.navigate(Routes.ABOUT) }
            )
        }
        composable(Routes.CATEGORIES) {
            LaunchedEffect(Unit) { appViewModel.onRouteChanged(Routes.CATEGORIES) }
            CategoriesScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.ABOUT) {
            LaunchedEffect(Unit) { appViewModel.onRouteChanged(Routes.ABOUT) }
            AboutScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
