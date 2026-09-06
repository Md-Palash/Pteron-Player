package com.pteron.player.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pteron.player.data.mock.MockFolderRepository
import com.pteron.player.ui.screens.folder.FolderDetailScreen
import com.pteron.player.ui.screens.home.HomeScreen
import com.pteron.player.ui.screens.settings.ThemeSettingsScreen
import com.pteron.player.ui.state.AppSettingsViewModel

// Short, subtle, no bounce — see design notes in Stage 1 write-up.
private const val TRANSITION_DURATION_MS = 220
private const val FOLDER_ARG = "folderId"

@Composable
fun PteronNavHost(
    settingsViewModel: AppSettingsViewModel,
    isDarkTheme: Boolean
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Destination.Home.route
    ) {
        composable(
            route = Destination.Home.route,
            exitTransition = {
                fadeOut(animationSpec = tween(TRANSITION_DURATION_MS)) +
                    scaleOut(targetScale = 0.96f, animationSpec = tween(TRANSITION_DURATION_MS))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(TRANSITION_DURATION_MS)) +
                    scaleIn(initialScale = 0.96f, animationSpec = tween(TRANSITION_DURATION_MS))
            }
        ) {
            HomeScreen(
                isDarkTheme = isDarkTheme,
                onFolderClick = { folder ->
                    navController.navigate(Destination.FolderDetail.createRoute(folder.id))
                },
                onSettingsClick = {
                    navController.navigate(Destination.ThemeSettings.route)
                }
            )
        }

        composable(
            route = Destination.FolderDetail.route,
            arguments = listOf(navArgument(FOLDER_ARG) { type = NavType.StringType }),
            enterTransition = {
                fadeIn(animationSpec = tween(TRANSITION_DURATION_MS)) +
                    scaleIn(initialScale = 0.96f, animationSpec = tween(TRANSITION_DURATION_MS))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(TRANSITION_DURATION_MS)) +
                    scaleOut(targetScale = 0.96f, animationSpec = tween(TRANSITION_DURATION_MS))
            }
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getString(FOLDER_ARG)
            val folder = MockFolderRepository.getFolders().find { it.id == folderId }
            FolderDetailScreen(
                folder = folder,
                isDarkTheme = isDarkTheme,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Destination.ThemeSettings.route,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(TRANSITION_DURATION_MS)
                ) + fadeIn(animationSpec = tween(TRANSITION_DURATION_MS))
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(TRANSITION_DURATION_MS)
                ) + fadeOut(animationSpec = tween(TRANSITION_DURATION_MS))
            }
        ) {
            ThemeSettingsScreen(
                settingsViewModel = settingsViewModel,
                isDarkTheme = isDarkTheme,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
