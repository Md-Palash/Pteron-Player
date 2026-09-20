package com.pteron.player.ui.player

import android.Manifest
import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.pteron.player.data.model.AspectRatioMode
import com.pteron.player.data.prefs.OrientationLock
import com.pteron.player.theme.forVideoOverlay
import com.pteron.player.ui.player.components.BrightnessHud
import com.pteron.player.ui.player.components.GestureOverlay
import com.pteron.player.ui.player.components.PlayerBottomBar
import com.pteron.player.ui.player.components.PlayerTopBar
import com.pteron.player.ui.player.components.SeekFlashIndicator
import com.pteron.player.ui.player.components.SeekFlashSide
import com.pteron.player.ui.player.components.SpeedSelectorSheet
import com.pteron.player.ui.player.components.TrackSelectorSheet
import com.pteron.player.ui.player.components.VolumeHud
import com.pteron.player.util.PipController
import com.pteron.player.util.formatTimecode
import kotlinx.coroutines.delay

/** The notification permission is only asked once per app launch, never on every video. */
private var notificationPermissionAsked = false

@UnstableApi
@Composable
fun PlayerScreen(
    videoId: Long,
    bucketId: String,
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    /** Set when the video was opened from another app; [videoId]/[bucketId] are ignored then. */
    externalUri: String? = null,
    /** Start the folder queue with shuffle on ("Shuffle play"). */
    shuffle: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val isInPip = rememberIsInPipMode(activity)

    val subtitlePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val displayName = queryDisplayName(context, uri) ?: uri.lastPathSegment ?: "subtitle"
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.loadExternalSubtitle(uri, displayName)
        }
    }

    // Without POST_NOTIFICATIONS (Android 13+) the system simply doesn't show the media
    // notification, so ask once, the first time a video is opened.
    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { /* Nothing to do: if denied, playback works exactly the same, just without the notification. */ }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !notificationPermissionAsked &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionAsked = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var controlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showAudioSheet by remember { mutableStateOf(false) }
    var showSubtitleSheet by remember { mutableStateOf(false) }
    var flashSide by remember { mutableStateOf(SeekFlashSide.NONE) }
    var scrubPreviewMs by remember { mutableStateOf<Long?>(null) }

    var brightnessLevel by remember { mutableStateOf(0.5f) }
    var showBrightnessHud by remember { mutableStateOf(false) }
    var volumeFraction by remember { mutableStateOf(0.5f) }
    var volumeCurrent by remember { mutableStateOf(0) }
    var volumeMax by remember { mutableStateOf(1) }
    var showVolumeHud by remember { mutableStateOf(false) }

    // Each HUD auto-hides ~900ms after the last change, restarting whenever the
    // level changes again (LaunchedEffect keyed on the value itself does this for free).
    LaunchedEffect(brightnessLevel) {
        if (showBrightnessHud) {
            delay(900)
            showBrightnessHud = false
        }
    }
    LaunchedEffect(volumeFraction) {
        if (showVolumeHud) {
            delay(900)
            showVolumeHud = false
        }
    }

    LaunchedEffect(videoId, bucketId, externalUri, shuffle) {
        if (externalUri != null) {
            viewModel.openExternal(Uri.parse(externalUri))
        } else {
            viewModel.openVideo(videoId, bucketId, shuffle)
        }
    }

    // Minimized / screen off: stop decoding video (audio continues through the media
    // notification). Closing the Picture-in-Picture window is the one case that should
    // stop playback: the system reports it as a stop while still in PiP mode.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        if (activity?.isInPictureInPictureMode == true) viewModel.pause()
        viewModel.onUiVisibilityChanged(false)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        viewModel.onUiVisibilityChanged(true)
    }

    // The mini window has no room for controls: clear everything that could be on screen.
    LaunchedEffect(isInPip) {
        if (isInPip) {
            controlsVisible = false
            showSpeedSheet = false
            showAudioSheet = false
            showSubtitleSheet = false
            showBrightnessHud = false
            showVolumeHud = false
            scrubPreviewMs = null
        }
    }

    // True fullscreen: hides the status bar and navigation bar while the player is open,
    // restoring them on exit. Swipe-from-edge still temporarily reveals the system bars
    // (BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE), which is the expected immersive-mode gesture.
    DisposableEffect(Unit) {
        val window = activity?.window
        val controller = window?.let { androidx.core.view.WindowCompat.getInsetsController(it, it.decorView) }
        controller?.let {
            it.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            it.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose {
            controller?.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
    }

    // Keep the screen awake while a video is actively playing (MX Player-style),
    // governed by the "Keep screen on" Settings toggle.
    DisposableEffect(uiState.isPlaying, uiState.playbackPrefs.keepScreenOnWhilePlaying) {
        val window = activity?.window
        if (window != null && uiState.isPlaying && uiState.playbackPrefs.keepScreenOnWhilePlaying) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Applies the player's orientation-lock preference for as long as this screen
    // is open, restoring the device's normal (unlocked) behavior on exit.
    DisposableEffect(uiState.playbackPrefs.orientationLock) {
        activity?.requestedOrientation = when (uiState.playbackPrefs.orientationLock) {
            OrientationLock.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            OrientationLock.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            OrientationLock.AUTO -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Auto-hide controls after a few seconds of inactivity while playing and unlocked.
    LaunchedEffect(controlsVisible, uiState.isPlaying, isLocked) {
        if (controlsVisible && uiState.isPlaying && !isLocked) {
            delay(uiState.playbackPrefs.controlAutoHideSeconds * 1000L)
            controlsVisible = false
        }
    }

    // The video's real aspect ratio for the PiP window is published by PlayerViewModel
    // (from ExoPlayer's rotation-aware video size), so only eligibility is tracked here.
    DisposableEffect(uiState.isPlaying) {
        PipController.isEligibleForAutoPip.value = uiState.isPlaying
        onDispose { PipController.isEligibleForAutoPip.value = false }
    }

    // The AndroidView's update block re-runs whenever a state it reads changes. Reading these
    // derived values (instead of the whole uiState) means it only re-runs when one of these
    // three actually changes, not on every unrelated state update.
    // The theme's accent, lifted if needed so it stays visible on top of the black video.
    val themeAccent = MaterialTheme.colorScheme.primary
    val playerAccent = remember(themeAccent) { themeAccent.forVideoOverlay() }

    val aspectMode by remember { derivedStateOf { uiState.aspectRatioMode } }
    val subtitlesEnabled by remember { derivedStateOf { uiState.subtitlesEnabled } }
    val subtitleTextSize by remember { derivedStateOf { uiState.playbackPrefs.subtitleTextSizeSp } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    useController = false
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    player = viewModel.player
                }
            },
            update = { playerView ->
                if (playerView.player !== viewModel.player) playerView.player = viewModel.player

                // The small PiP window always shows the whole picture, whatever mode is selected.
                val mode = if (isInPip) AspectRatioMode.FIT else aspectMode
                val resizeMode = when (mode) {
                    AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    AspectRatioMode.CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    AspectRatioMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                }
                if (playerView.resizeMode != resizeMode) playerView.resizeMode = resizeMode

                playerView.subtitleView?.let { subtitleView ->
                    val visibility = if (subtitlesEnabled) android.view.View.VISIBLE else android.view.View.GONE
                    if (subtitleView.visibility != visibility) subtitleView.visibility = visibility
                    subtitleView.setFixedTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, subtitleTextSize)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (!isInPip) {
            // The position changes every second; reading it inside WithProgress keeps that
            // recomposition local to these two layers instead of the whole player screen.
            WithProgress(viewModel) { progress ->
                GestureOverlay(
                    modifier = Modifier.fillMaxSize(),
                    locked = isLocked,
                    sensitivity = uiState.appearance.gestureSensitivity,
                    durationMs = uiState.durationMs,
                    currentPositionMs = progress.positionMs,
                    onToggleControls = { controlsVisible = !controlsVisible },
                    onSeekBy = viewModel::seekBy,
                    seekStepMs = viewModel.seekStepMs(),
                    onScrubPreview = { scrubPreviewMs = it },
                    onScrubCommit = { viewModel.seekTo(it) },
                    onFlash = { flashSide = it },
                    onBrightnessChanged = {
                        brightnessLevel = it
                        showBrightnessHud = true
                    },
                    onVolumeChanged = { fraction, current, max ->
                        volumeFraction = fraction
                        volumeCurrent = current
                        volumeMax = max
                        showVolumeHud = true
                    }
                )
            }

            BrightnessHud(
                level = brightnessLevel,
                visible = showBrightnessHud,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 24.dp)
            )
            VolumeHud(
                fraction = volumeFraction,
                current = volumeCurrent,
                max = volumeMax,
                visible = showVolumeHud,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 24.dp)
            )

            SeekFlashIndicator(
                side = flashSide,
                seekSeconds = uiState.playbackPrefs.doubleTapSeekSeconds,
                modifier = Modifier.align(Alignment.Center)
            )

            scrubPreviewMs?.let { previewMs ->
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color.Black.copy(alpha = 0.6f), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = formatTimecode(previewMs),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        if (uiState.isBuffering) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        }

        AnimatedVisibility(
            visible = controlsVisible && !isLocked && !isInPip,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            PlayerTopBar(
                title = uiState.currentVideo?.displayName ?: "",
                isFavorite = uiState.currentVideo?.isFavorite ?: false,
                isFourK = uiState.currentVideo?.isFourK ?: false,
                isHardwareDecoder = uiState.isHardwareDecoder,
                frameRate = uiState.frameRate,
                selectedAudioLabel = uiState.audioTracks.firstOrNull { it.isSelected }?.label,
                selectedSubtitleLabel = uiState.subtitleTracks.firstOrNull { it.isSelected }?.label,
                onBack = onBack,
                onToggleFavorite = viewModel::toggleFavoriteCurrent,
                onOpenAudioTracks = { showAudioSheet = true },
                onOpenSubtitleTracks = { showSubtitleSheet = true }
            )
        }

        AnimatedVisibility(
            visible = controlsVisible && !isLocked && !isInPip,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            WithProgress(viewModel) { progress ->
                PlayerBottomBar(
                    isPlaying = uiState.isPlaying,
                    currentPositionMs = scrubPreviewMs ?: progress.positionMs,
                    durationMs = uiState.durationMs,
                    bufferedPercentage = progress.bufferedPercentage,
                    playbackSpeed = uiState.playbackSpeed,
                    isLocked = isLocked,
                    hasNext = uiState.hasNext,
                    hasPrevious = uiState.hasPrevious,
                    isMuted = uiState.isMuted,
                    repeatMode = uiState.repeatMode,
                    shuffleEnabled = uiState.shuffleEnabled,
                    accentColor = playerAccent,
                    seekStepMs = viewModel.seekStepMs(),
                    onScrub = { viewModel.seekTo(it) },
                    onPlayPause = viewModel::playPause,
                    onSeekBy = viewModel::seekBy,
                    onNext = viewModel::skipToNext,
                    onPrevious = viewModel::skipToPrevious,
                    onOpenSpeedMenu = { showSpeedSheet = true },
                    onToggleLock = { isLocked = !isLocked; controlsVisible = true },
                    onEnterPip = {
                        activity?.let { enterPictureInPicture(it) }
                    },
                    onToggleRotation = {
                        activity?.let { toggleOrientation(it) }
                    },
                    onCycleAspectRatio = viewModel::cycleAspectRatio,
                    onToggleMute = viewModel::toggleMute,
                    onCycleRepeat = viewModel::cycleRepeatMode,
                    onToggleShuffle = viewModel::toggleShuffle
                )
            }
        }

        if (isLocked && !isInPip) {
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                androidx.compose.material3.IconButton(onClick = { isLocked = false; controlsVisible = true }) {
                    androidx.compose.material3.Icon(
                        Icons.Outlined.Lock,
                        contentDescription = "Unlock controls",
                        tint = Color.White
                    )
                }
            }
        }

        uiState.error?.let { message ->
            androidx.compose.material3.AlertDialog(
                onDismissRequest = viewModel::clearError,
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = viewModel::clearError) { Text("OK") }
                },
                title = { Text("Playback error") },
                text = { Text(message) }
            )
        }
    }

    if (showSpeedSheet) {
        SpeedSelectorSheet(
            currentSpeed = uiState.playbackSpeed,
            onSelect = {
                viewModel.setPlaybackSpeed(it)
                showSpeedSheet = false
            },
            onDismiss = { showSpeedSheet = false }
        )
    }
    if (showAudioSheet) {
        TrackSelectorSheet(
            title = "Audio track",
            tracks = uiState.audioTracks,
            allowDisable = false,
            onSelect = { option ->
                option?.let { viewModel.selectAudioTrack(it) }
                showAudioSheet = false
            },
            onDismiss = { showAudioSheet = false }
        )
    }
    if (showSubtitleSheet) {
        TrackSelectorSheet(
            title = "Subtitles",
            tracks = uiState.subtitleTracks,
            allowDisable = true,
            onSelect = { option ->
                viewModel.selectSubtitleTrack(option)
                showSubtitleSheet = false
            },
            onDismiss = { showSubtitleSheet = false },
            onLoadExternal = {
                showSubtitleSheet = false
                subtitlePickerLauncher.launch(arrayOf("*/*"))
            }
        )
    }
}

/**
 * Collects the fast-changing playback position here, in its own recompose scope, so a
 * once-per-second tick only redraws [content] and not the entire player screen.
 */
@Composable
private fun WithProgress(viewModel: PlayerViewModel, content: @Composable (PlayerProgress) -> Unit) {
    val progress by viewModel.progress.collectAsState()
    content(progress)
}

/** True while the activity is shown in the system's Picture-in-Picture window. */
@Composable
private fun rememberIsInPipMode(activity: Activity?): Boolean {
    var inPip by remember { mutableStateOf(activity?.isInPictureInPictureMode == true) }
    DisposableEffect(activity) {
        val componentActivity = activity as? ComponentActivity
        val listener = Consumer<PictureInPictureModeChangedInfo> { info ->
            inPip = info.isInPictureInPictureMode
        }
        componentActivity?.addOnPictureInPictureModeChangedListener(listener)
        onDispose { componentActivity?.removeOnPictureInPictureModeChangedListener(listener) }
    }
    return inPip
}

private fun toggleOrientation(activity: Activity) {
    activity.requestedOrientation = if (activity.resources.configuration.orientation ==
        android.content.res.Configuration.ORIENTATION_LANDSCAPE
    ) {
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    } else {
        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }
}

private fun enterPictureInPicture(activity: Activity) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        val aspect = PipController.videoAspectRatio.value.coerceIn(0.42f, 2.39f)
        val params = android.app.PictureInPictureParams.Builder()
            .setAspectRatio(android.util.Rational((aspect * 100).toInt(), 100))
            .build()
        activity.enterPictureInPictureMode(params)
    }
}

/** Resolves a human-readable file name for a SAF-picked Uri, for the subtitle-track label. */
private fun queryDisplayName(context: android.content.Context, uri: android.net.Uri): String? {
    return runCatching {
        context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
    }.getOrNull()
}
