package com.pteron.player.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pteron.player.PteronApp
import com.pteron.player.ui.folder.FolderScreen
import com.pteron.player.ui.folder.FolderViewModel
import com.pteron.player.ui.library.LibraryScreen
import com.pteron.player.ui.library.LibraryViewModel
import com.pteron.player.ui.player.PlayerScreen
import com.pteron.player.ui.player.PlayerViewModel
import com.pteron.player.ui.settings.SettingsScreen
import com.pteron.player.ui.settings.SettingsViewModel

@UnstableApi
@Composable
fun PteronNavGraph(app: PteronApp) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Library.route) {
        composable(Screen.Library.route) {
            val viewModel: LibraryViewModel = viewModel(
                factory = LibraryViewModel.Factory(
                    app.mediaStoreRepository, app.playbackStateRepository, app.appearancePrefsRepository
                )
            )
            LibraryScreen(
                viewModel = viewModel,
                onOpenFolder = { bucketId, name -> navController.navigate(Screen.Folder.createRoute(bucketId, name)) },
                onOpenVideo = { videoId, bucketId -> navController.navigate(Screen.Player.createRoute(videoId, bucketId)) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(app.appearancePrefsRepository)
            )
            SettingsScreen(viewModel = viewModel, onBack = navController::popBackStack)
        }

        composable(
            route = Screen.Folder.route,
            arguments = listOf(
                navArgument("bucketId") { type = NavType.StringType },
                navArgument("folderName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val bucketId = backStackEntry.arguments?.getString("bucketId").orEmpty()
            val folderName = backStackEntry.arguments?.getString("folderName").orEmpty()
            val viewModel: FolderViewModel = viewModel(
                factory = FolderViewModel.Factory(
                    bucketId, folderName, app.mediaStoreRepository, app.playbackStateRepository, app.appearancePrefsRepository
                )
            )
            FolderScreen(
                viewModel = viewModel,
                onBack = navController::popBackStack,
                onOpenVideo = { videoId, folderBucketId -> navController.navigate(Screen.Player.createRoute(videoId, folderBucketId)) },
                onShufflePlay = { videoId, folderBucketId -> navController.navigate(Screen.Player.createRoute(videoId, folderBucketId)) }
            )
        }

        composable(
            route = Screen.Player.route,
            arguments = listOf(
                navArgument("videoId") { type = NavType.LongType },
                navArgument("bucketId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val videoId = backStackEntry.arguments?.getLong("videoId") ?: return@composable
            val bucketId = backStackEntry.arguments?.getString("bucketId").orEmpty()
            val viewModel: PlayerViewModel = viewModel(
                factory = PlayerViewModel.Factory(
                    app, app.mediaStoreRepository, app.playbackStateRepository, app.appearancePrefsRepository
                )
            )
            PlayerScreen(
                videoId = videoId,
                bucketId = bucketId,
                viewModel = viewModel,
                onBack = navController::popBackStack
            )
        }
    }
}
