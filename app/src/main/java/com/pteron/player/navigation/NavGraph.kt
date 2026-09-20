package com.pteron.player.navigation

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
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
import com.pteron.player.ui.playlists.PlaylistsScreen
import com.pteron.player.ui.settings.SettingsScreen
import com.pteron.player.ui.settings.SettingsViewModel
import com.pteron.player.ui.videos.VideosScreen
import com.pteron.player.ui.videos.VideosViewModel

/**
 * Standard single-top bottom-nav pattern: switching tabs pops back to the
 * graph's start destination and reuses a single instance per tab instead of
 * stacking up duplicate copies of Library/Videos/Playlists/Settings.
 */
private fun NavHostController.navigateToTab(destination: BottomNavDestination) {
    navigate(destination.screen.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** A gentle slide-and-fade used for every push (Folder, Player) so navigation
 *  feels like motion rather than an instant cut. Tab switches (bottom nav)
 *  intentionally don't use this -- see [navigateToTab] -- since a lateral
 *  push motion would read oddly for switching between top-level sections. */
private val pushEnter = slideInHorizontally(animationSpec = tween(280)) { it / 4 } + fadeIn(tween(220))
private val pushExit = fadeOut(tween(160))
private val popEnter = fadeIn(tween(200))
private val popExit = slideOutHorizontally(animationSpec = tween(280)) { it / 4 } + fadeOut(tween(220))

@UnstableApi
@Composable
private fun rememberPlayerViewModel(app: PteronApp): PlayerViewModel = viewModel(
    factory = PlayerViewModel.Factory(
        app, app.mediaStoreRepository, app.playbackStateRepository, app.appearancePrefsRepository, app.playbackPrefsRepository
    )
)

/**
 * @param externalVideoUri a video another app asked us to open (ACTION_VIEW). When non-null it is
 *  opened in the player on top of the normal back stack, then [onExternalVideoHandled] is called so
 *  the same request is never opened twice.
 */
@UnstableApi
@Composable
fun PteronNavGraph(
    app: PteronApp,
    externalVideoUri: Uri? = null,
    onExternalVideoHandled: () -> Unit = {}
) {
    val navController = rememberNavController()

    LaunchedEffect(externalVideoUri) {
        if (externalVideoUri != null) {
            val current = navController.currentDestination?.route
            navController.navigate(Screen.ExternalPlayer.createRoute(externalVideoUri.toString())) {
                // Replace a player that is already open instead of stacking a second one
                // (two ExoPlayers alive at once would both play and fight over the session).
                if (current != null &&
                    (current == Screen.Player.route || current == Screen.ExternalPlayer.route)
                ) {
                    popUpTo(current) { inclusive = true }
                }
            }
            onExternalVideoHandled()
        }
    }

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
                onNavigate = navController::navigateToTab
            )
        }

        composable(Screen.Videos.route) {
            val viewModel: VideosViewModel = viewModel(
                factory = VideosViewModel.Factory(
                    app.mediaStoreRepository, app.playbackStateRepository, app.appearancePrefsRepository
                )
            )
            VideosScreen(
                viewModel = viewModel,
                onOpenVideo = { videoId, bucketId -> navController.navigate(Screen.Player.createRoute(videoId, bucketId)) },
                onNavigate = navController::navigateToTab
            )
        }

        composable(Screen.Playlists.route) {
            PlaylistsScreen(onNavigate = navController::navigateToTab)
        }

        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(
                    app.appearancePrefsRepository, app.playbackPrefsRepository, app.playbackStateRepository
                )
            )
            SettingsScreen(viewModel = viewModel, onNavigate = navController::navigateToTab)
        }

        composable(
            route = Screen.Folder.route,
            arguments = listOf(
                navArgument("bucketId") { type = NavType.StringType },
                navArgument("folderName") { type = NavType.StringType }
            ),
            enterTransition = { pushEnter },
            exitTransition = { pushExit },
            popEnterTransition = { popEnter },
            popExitTransition = { popExit }
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
                onShufflePlay = { videoId, folderBucketId ->
                    navController.navigate(Screen.Player.createRoute(videoId, folderBucketId, shuffle = true))
                }
            )
        }

        composable(
            route = Screen.Player.route,
            arguments = listOf(
                navArgument("videoId") { type = NavType.LongType },
                navArgument("bucketId") { type = NavType.StringType },
                navArgument("shuffle") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            ),
            enterTransition = { pushEnter },
            exitTransition = { pushExit },
            popEnterTransition = { popEnter },
            popExitTransition = { popExit }
        ) { backStackEntry ->
            val videoId = backStackEntry.arguments?.getLong("videoId") ?: return@composable
            val bucketId = backStackEntry.arguments?.getString("bucketId").orEmpty()
            val shuffle = backStackEntry.arguments?.getBoolean("shuffle") ?: false
            PlayerScreen(
                videoId = videoId,
                bucketId = bucketId,
                shuffle = shuffle,
                viewModel = rememberPlayerViewModel(app),
                onBack = navController::popBackStack
            )
        }

        composable(
            route = Screen.ExternalPlayer.route,
            arguments = listOf(navArgument("uri") { type = NavType.StringType }),
            enterTransition = { pushEnter },
            exitTransition = { pushExit },
            popEnterTransition = { popEnter },
            popExitTransition = { popExit }
        ) { backStackEntry ->
            val uri = backStackEntry.arguments?.getString("uri") ?: return@composable
            PlayerScreen(
                videoId = -1L,
                bucketId = "",
                externalUri = uri,
                viewModel = rememberPlayerViewModel(app),
                onBack = navController::popBackStack
            )
        }
    }
}
