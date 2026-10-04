package com.pteron.player.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import com.pteron.player.ui.common.PteronBottomNavBar
import com.pteron.player.util.TabReselect
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
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
import com.pteron.player.ui.playlists.PlaylistDetailScreen
import com.pteron.player.ui.playlists.PlaylistDetailViewModel
import com.pteron.player.ui.playlists.PlaylistsScreen
import com.pteron.player.ui.playlists.PlaylistsViewModel
import com.pteron.player.ui.settings.SettingsScreen
import com.pteron.player.ui.settings.SettingsViewModel
import com.pteron.player.ui.videos.VideosScreen
import com.pteron.player.ui.videos.VideosViewModel
import kotlinx.coroutines.launch

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
/**
 * Defaults for every destination that doesn't set its own (the four tabs). Navigation Compose's
 * built-in default is a 700 ms fade, which made returning from a folder or the player feel sluggish;
 * these are short and the incoming screen waits a beat so the two never muddy each other.
 */
private val tabEnter = fadeIn(tween(200, delayMillis = 50))
private val tabExit = fadeOut(tween(110))
private val tabPopEnter = fadeIn(tween(200, delayMillis = 50))
private val tabPopExit = fadeOut(tween(110))

private val pushEnter = slideInHorizontally(animationSpec = tween(280)) { it / 4 } + fadeIn(tween(220))
private val pushExit = fadeOut(tween(160))
private val popEnter = fadeIn(tween(200))
private val popExit = slideOutHorizontally(animationSpec = tween(280)) { it / 4 } + fadeOut(tween(220))

@UnstableApi
@Composable
private fun rememberPlayerViewModel(app: PteronApp): PlayerViewModel = viewModel(
    factory = PlayerViewModel.Factory(
        app, app.mediaStoreRepository, app.playbackStateRepository, app.appearancePrefsRepository,
        app.playbackPrefsRepository, app.playlistRepository
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

    // One bottom bar for the whole app, above the navigation host. Previously every tab drew its own
    // copy, so switching tabs cross-faded two bars on top of each other (a visible flicker).
    val navEntry by navController.currentBackStackEntryAsState()
    val route = navEntry?.destination?.route
    val currentTab = BottomNavDestination.entries.firstOrNull { it.screen.route == route }
    var lastTab by remember { mutableStateOf(BottomNavDestination.LIBRARY) }
    // Kept so the bar still shows the right highlight while it slides away.
    SideEffect { if (currentTab != null) lastTab = currentTab }

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

    Box {
    NavHost(
        navController = navController,
        startDestination = Screen.Library.route,
        enterTransition = { tabEnter },
        exitTransition = { tabExit },
        popEnterTransition = { tabPopEnter },
        popExitTransition = { tabPopExit }
    ) {
        composable(Screen.Library.route) {
            val viewModel: LibraryViewModel = viewModel(
                factory = LibraryViewModel.Factory(
                    app.mediaStoreRepository, app.playbackStateRepository, app.appearancePrefsRepository
                )
            )
            LibraryScreen(
                viewModel = viewModel,
                onOpenFolder = { bucketId, name -> navController.navigate(Screen.Folder.createRoute(bucketId, name)) },
                onOpenVideo = { videoId, bucketId -> navController.navigate(Screen.Player.createRoute(videoId, bucketId)) }
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
                onOpenVideo = { videoId, bucketId -> navController.navigate(Screen.Player.createRoute(videoId, bucketId)) }
            )
        }

        composable(Screen.Playlists.route) {
            val scope = rememberCoroutineScope()
            val viewModel: PlaylistsViewModel = viewModel(
                factory = PlaylistsViewModel.Factory(app.playlistRepository)
            )
            PlaylistsScreen(
                viewModel = viewModel,
                onOpenPlaylist = { id -> navController.navigate(Screen.PlaylistDetail.createRoute(id)) },
                onToggleDarkMode = { scope.launch { app.appearancePrefsRepository.toggleDarkMode() } }
            )
        }

        composable(
            route = Screen.PlaylistDetail.route,
            arguments = listOf(navArgument("playlistId") { type = NavType.LongType }),
            enterTransition = { pushEnter },
            exitTransition = { pushExit },
            popEnterTransition = { popEnter },
            popExitTransition = { popExit }
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: return@composable
            val viewModel: PlaylistDetailViewModel = viewModel(
                factory = PlaylistDetailViewModel.Factory(playlistId, app.playlistRepository, app.mediaStoreRepository)
            )
            PlaylistDetailScreen(
                viewModel = viewModel,
                onBack = navController::popBackStack,
                onPlay = { video, shuffle ->
                    navController.navigate(
                        Screen.Player.createRoute(video.id, video.bucketId, shuffle = shuffle, playlistId = playlistId)
                    )
                }
            )
        }

        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(
                    app.appearancePrefsRepository, app.playbackPrefsRepository, app.playbackStateRepository
                )
            )
            SettingsScreen(viewModel = viewModel)
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
                },
                navArgument("playlistId") {
                    type = NavType.LongType
                    defaultValue = -1L
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
            val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: -1L
            PlayerScreen(
                videoId = videoId,
                bucketId = bucketId,
                shuffle = shuffle,
                playlistId = playlistId,
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

    AnimatedVisibility(
        visible = currentTab != null,
        enter = fadeIn(tween(200)) + slideInVertically(tween(260)) { it },
        exit = fadeOut(tween(140)) + slideOutVertically(tween(200)) { it },
        modifier = Modifier.align(Alignment.BottomCenter)
    ) {
        PteronBottomNavBar(
            current = currentTab ?: lastTab,
            onSelect = { destination ->
                if (destination == currentTab) {
                    TabReselect.events.tryEmit(destination)
                } else {
                    navController.navigateToTab(destination)
                }
            },
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 40.dp, vertical = 18.dp)
        )
    }
    }
}
