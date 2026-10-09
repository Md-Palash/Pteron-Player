package com.pteron.player.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pteron.player.PteronApp
import com.pteron.player.ui.audio.AudioArtistScreen
import com.pteron.player.ui.audio.AudioPlaylistScreen
import com.pteron.player.ui.audio.AudioScreen
import com.pteron.player.ui.audio.AudioViewModel
import com.pteron.player.ui.audio.NowPlayingScreen
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
import kotlin.math.sign

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
// The incoming tab fades up from slightly smaller while the outgoing one fades quickly underneath,
// so there is never an empty frame between the two (the old 50 ms delay showed the bare window).
private val tabFadeScaleEnter = fadeIn(tween(240, easing = FastOutSlowInEasing)) +
    scaleIn(tween(240, easing = FastOutSlowInEasing), initialScale = 0.97f)

private fun tabIndex(route: String?): Int = BottomNavDestination.entries.indexOfFirst { it.screen.route == route }

/** -1 / +1 when moving between two bottom-bar tabs (left / right), 0 for anything else. */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabDirection(): Int {
    val from = tabIndex(initialState.destination.route)
    val to = tabIndex(targetState.destination.route)
    return if (from < 0 || to < 0 || from == to) 0 else sign((to - from).toFloat()).toInt()
}

// Moving between tabs: the new tab slides in from the side of its icon while fading up, and the old
// one drifts the other way while fading out, so the bar and the content read as one gesture.
// Non-tab neighbours (coming back from a folder or the player) keep the plain fade + scale.
private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabEnterFor(): EnterTransition {
    val dir = tabDirection()
    if (dir == 0) return tabFadeScaleEnter
    return slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { dir * it / 6 } +
        fadeIn(tween(260, easing = FastOutSlowInEasing))
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabExitFor(): ExitTransition {
    val dir = tabDirection()
    // No fade for non-tab neighbours: the outgoing screen stays opaque until the new one covers it.
    if (dir == 0) return ExitTransition.None
    return slideOutHorizontally(tween(340, easing = FastOutSlowInEasing)) { -dir * it / 8 } +
        fadeOut(tween(200, easing = FastOutSlowInEasing))
}

// The video surface cannot slide or fade with the rest of the screen, so a slide-out looked stuck.
// Leaving the player is a short plain fade instead.
private val playerPopExit = fadeOut(tween(120))

private val pushEnter = slideInHorizontally(animationSpec = tween(300, easing = FastOutSlowInEasing)) { it / 5 } +
    fadeIn(tween(240, easing = FastOutSlowInEasing))
private val pushExit = ExitTransition.None

// Going back mirrors going forward: the screen being left slides out to the right while it fades,
// and the screen underneath drifts in from the left (a short parallax) as it fades up, instead of
// popping in with a bare fade. Same easing on both so the two motions read as one gesture.
private val popEnter = slideInHorizontally(animationSpec = tween(300, easing = FastOutSlowInEasing)) { -it / 10 } +
    fadeIn(tween(240, easing = FastOutSlowInEasing))
private val popExit = slideOutHorizontally(animationSpec = tween(300, easing = FastOutSlowInEasing)) { it / 4 } +
    fadeOut(tween(220, easing = FastOutSlowInEasing))

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

    // One view model for the whole Audio section (tab, artist / playlist pages, Now Playing).
    val audioViewModel: AudioViewModel = viewModel(
        factory = AudioViewModel.Factory(
            app.audioRepository, app.audioStateRepository, app.audioPlaylistRepository,
            app.musicPrefsRepository, app.appearancePrefsRepository
        )
    )

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
        enterTransition = { tabEnterFor() },
        exitTransition = { tabExitFor() },
        popEnterTransition = { tabEnterFor() },
        popExitTransition = { tabExitFor() }
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

        composable(Screen.Audio.route) {
            AudioScreen(
                viewModel = audioViewModel,
                controller = app.audioController,
                onOpenArtist = { navController.navigate(Screen.AudioArtist.createRoute(it)) },
                onOpenPlaylist = { navController.navigate(Screen.AudioPlaylist.createRoute(it)) },
                onOpenNowPlaying = { navController.navigate(Screen.NowPlaying.route) }
            )
        }

        composable(
            route = Screen.AudioArtist.route,
            arguments = listOf(navArgument("artist") { type = NavType.StringType }),
            enterTransition = { pushEnter },
            exitTransition = { pushExit },
            popEnterTransition = { popEnter },
            popExitTransition = { popExit }
        ) { backStackEntry ->
            AudioArtistScreen(
                viewModel = audioViewModel,
                controller = app.audioController,
                artist = backStackEntry.arguments?.getString("artist").orEmpty(),
                onBack = navController::popBackStack,
                onOpenNowPlaying = { navController.navigate(Screen.NowPlaying.route) }
            )
        }

        composable(
            route = Screen.AudioPlaylist.route,
            arguments = listOf(navArgument("playlistId") { type = NavType.LongType }),
            enterTransition = { pushEnter },
            exitTransition = { pushExit },
            popEnterTransition = { popEnter },
            popExitTransition = { popExit }
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: return@composable
            AudioPlaylistScreen(
                viewModel = audioViewModel,
                controller = app.audioController,
                playlistId = playlistId,
                onBack = navController::popBackStack,
                onOpenNowPlaying = { navController.navigate(Screen.NowPlaying.route) }
            )
        }

        composable(
            route = Screen.NowPlaying.route,
            enterTransition = { pushEnter },
            exitTransition = { pushExit },
            popEnterTransition = { popEnter },
            popExitTransition = { popExit }
        ) {
            NowPlayingScreen(
                controller = app.audioController,
                viewModel = audioViewModel,
                onBack = navController::popBackStack
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
                factory = PlaylistDetailViewModel.Factory(
                    playlistId, app.playlistRepository, app.mediaStoreRepository, app.playbackStateRepository
                )
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
                    app.appearancePrefsRepository, app.playbackPrefsRepository, app.playbackStateRepository,
                    app.musicPrefsRepository, app.audioStateRepository
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
            popExitTransition = { playerPopExit }
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
            popExitTransition = { playerPopExit }
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
                .padding(horizontal = 28.dp, vertical = 18.dp)
        )
    }
    }
}
