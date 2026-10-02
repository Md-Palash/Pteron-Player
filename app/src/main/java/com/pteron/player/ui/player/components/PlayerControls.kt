package com.pteron.player.ui.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pteron.player.ui.player.RepeatMode
import com.pteron.player.util.formatRemaining
import com.pteron.player.util.formatTimecode
import kotlin.math.roundToInt

@Composable
fun PlayerTopBar(
    title: String,
    isFavorite: Boolean,
    isFourK: Boolean,
    isHardwareDecoder: Boolean?,
    frameRate: Float?,
    selectedAudioLabel: String?,
    selectedSubtitleLabel: String?,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenAudioTracks: () -> Unit,
    onOpenSubtitleTracks: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)))
            // Keeps the buttons clear of a camera cut-out and of the status bar when it is revealed.
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top))
            .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Exit player", tint = Color.White)
            }
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            )
            if (isFourK) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("4K", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
                androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
            }
            // A fade + tiny scale rather than an instant swap: the classification can settle a
            // moment after a video starts (or after ExoPlayer falls back to another decoder), and
            // popping the badge in/out abruptly read as a glitch rather than information arriving.
            AnimatedVisibility(
                visible = isHardwareDecoder != null,
                enter = fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.85f),
                exit = fadeOut(tween(120))
            ) {
                Row {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isHardwareDecoder == true) Color(0xFFD26938).copy(alpha = 0.85f)
                                else Color.White.copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            if (isHardwareDecoder == true) "HW+" else "SW",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                    androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
                }
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = Color.White
                )
            }
            IconButton(onClick = onOpenAudioTracks) {
                Icon(Icons.Outlined.Audiotrack, contentDescription = "Audio track", tint = Color.White)
            }
            IconButton(onClick = onOpenSubtitleTracks) {
                Icon(Icons.Outlined.Subtitles, contentDescription = "Subtitles", tint = Color.White)
            }
        }

        if (selectedAudioLabel != null || selectedSubtitleLabel != null || frameRate != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = listOfNotNull(
                        selectedAudioLabel?.let { "Track: $it" },
                        selectedSubtitleLabel?.let { "Sub: $it" }
                    ).joinToString("   •   "),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (frameRate != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("${frameRate.roundToInt()} FPS", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerBottomBar(
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    bufferedPercentage: Int,
    playbackSpeed: Float,
    isLocked: Boolean,
    hasNext: Boolean,
    hasPrevious: Boolean,
    isMuted: Boolean,
    repeatMode: RepeatMode,
    shuffleEnabled: Boolean,
    accentColor: Color,
    seekStepMs: Long,
    /** Finger on the seek bar: the position it points at, or null when the drag was cancelled. */
    onScrubPreview: (Long?) -> Unit,
    /** Finger lifted (or the bar was tapped): seek here. */
    onScrubCommit: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onSeekBy: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onOpenSpeedMenu: () -> Unit,
    onToggleLock: () -> Unit,
    onEnterPip: () -> Unit,
    onToggleRotation: () -> Unit,
    onCycleAspectRatio: () -> Unit,
    onToggleMute: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val seekSeconds = (seekStepMs / 1000L).toInt()
    val activeTint = Color.White
    val dimTint = Color.White.copy(alpha = 0.35f)
    val speedText = playbackSpeed.toString().removeSuffix(".0") + "x"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)
    ) {
        Scrubber(
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            bufferedPercentage = bufferedPercentage,
            accentColor = accentColor,
            onPreview = onScrubPreview,
            onCommit = onScrubCommit
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatTimecode(currentPositionMs), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium)
            Text(
                if (durationMs > 0L) formatRemaining(durationMs - currentPositionMs) else "",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelMedium
            )
        }

        // The main transport controls, always grouped around the play button.
        val transport: @Composable () -> Unit = {
            BarIconButton(Icons.Outlined.Shuffle, "Shuffle", if (shuffleEnabled) accentColor else dimTint, onClick = onToggleShuffle)
            BarIconButton(Icons.Outlined.SkipPrevious, "Previous", if (hasPrevious) activeTint else dimTint, enabled = hasPrevious, onClick = onPrevious)
            BarIconButton(Icons.Outlined.Replay, "Rewind $seekSeconds seconds", activeTint, onClick = { onSeekBy(-seekStepMs) })
            PlayPauseButton(isPlaying = isPlaying, accentColor = accentColor, onClick = onPlayPause)
            BarIconButton(Icons.Outlined.FastForward, "Forward $seekSeconds seconds", activeTint, onClick = { onSeekBy(seekStepMs) })
            BarIconButton(Icons.Outlined.SkipNext, "Next", if (hasNext) activeTint else dimTint, enabled = hasNext, onClick = onNext)
            BarIconButton(
                if (repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Outlined.Repeat,
                "Repeat: ${repeatMode.label}",
                if (repeatMode == RepeatMode.OFF) dimTint else accentColor,
                onClick = onCycleRepeat
            )
        }
        val toolsStart: @Composable () -> Unit = {
            TextIconChip(text = speedText, icon = Icons.Outlined.Speed, onClick = onOpenSpeedMenu)
            BarIconButton(
                if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                if (isMuted) "Unmute" else "Mute",
                activeTint,
                onClick = onToggleMute
            )
            BarIconButton(
                if (isLocked) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                "Lock controls",
                activeTint,
                onClick = onToggleLock
            )
        }
        val toolsEnd: @Composable () -> Unit = {
            BarIconButton(Icons.Outlined.PictureInPictureAlt, "Picture in picture", activeTint, onClick = onEnterPip)
            BarIconButton(Icons.Outlined.AspectRatio, "Cycle aspect ratio", activeTint, onClick = onCycleAspectRatio)
            BarIconButton(Icons.Outlined.ScreenRotation, "Rotate", activeTint, onClick = onToggleRotation)
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth >= 560.dp) {
                // Wide (landscape): one row, tools on both sides, play button exactly centered.
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) { toolsStart() }
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                        transport()
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) { toolsEnd() }
                }
            } else {
                // Narrow (portrait): transport centered on top, tools evenly spread underneath, so
                // nothing needs sideways scrolling and the play button stays in the middle.
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) { transport() }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        toolsStart()
                        toolsEnd()
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayPauseButton(isPlaying: Boolean, accentColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(horizontal = 6.dp)
            .clip(CircleShape)
            .background(accentColor)
            .size(52.dp),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick) {
            androidx.compose.animation.Crossfade(
                targetState = isPlaying,
                animationSpec = tween(150),
                label = "playPauseIcon"
            ) { playing ->
                Icon(
                    imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    // Readable on any accent: white on deep accents, dark on bright ones.
                    tint = if (accentColor.luminance() > 0.4f) Color.Black else Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun BarIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = modifier.size(40.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun TextIconChip(text: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.15f))
            .pointerInput(Unit) { detectTapGestures { onClick() } }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(icon, contentDescription = "Playback speed", tint = Color.White, modifier = Modifier.size(14.dp))
        Text(text, color = Color.White, style = MaterialTheme.typography.labelMedium)
    }
}

/** Effectively instant: used to keep the scrubber's fill glued to the finger while dragging. */
private val snapSpec = tween<Float>(durationMillis = 0)

/**
 * The seek bar. While a finger is on it nothing is sent to the player: the bar and the time
 * labels follow the finger through [onPreview], and the player seeks exactly once, in
 * [onCommit], when the finger lifts (or immediately for a plain tap). Seeking on every pixel of
 * movement made ExoPlayer re-seek dozens of times a second, which is what made scrubbing stutter.
 */
@Composable
private fun Scrubber(
    currentPositionMs: Long,
    durationMs: Long,
    bufferedPercentage: Int,
    accentColor: Color,
    onPreview: (Long?) -> Unit,
    onCommit: (Long) -> Unit
) {
    val progress = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    val buffered = (bufferedPercentage / 100f).coerceIn(0f, 1f)

    // While the finger is on the bar the fill must track it exactly -- animating would make it
    // lag a beat behind the touch. The glide is only for the normal once-a-second playback tick.
    var isDragging by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = if (isDragging) snapSpec else tween(durationMillis = 250),
        label = "scrubberProgress"
    )
    val animatedBuffered by animateFloatAsState(
        targetValue = buffered,
        animationSpec = tween(durationMillis = 250),
        label = "scrubberBuffered"
    )
    val thumbSize by animateDpAsState(if (isDragging) 18.dp else 12.dp, tween(120), label = "scrubberThumb")
    val thumbPx = with(LocalDensity.current) { thumbSize.toPx() }

    var trackWidthPx by remember { mutableFloatStateOf(1f) }
    val durationNow by rememberUpdatedState(durationMs)
    val previewNow by rememberUpdatedState(onPreview)
    val commitNow by rememberUpdatedState(onCommit)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .onGloballyPositioned { trackWidthPx = it.size.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    if (durationNow > 0) {
                        val fraction = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        commitNow((fraction * durationNow).toLong())
                    }
                }
            }
            .pointerInput(Unit) {
                var lastTargetMs = 0L
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        if (durationNow > 0) {
                            lastTargetMs = ((offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f) * durationNow).toLong()
                            previewNow(lastTargetMs)
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        if (durationNow > 0) commitNow(lastTargetMs) else previewNow(null)
                    },
                    onDragCancel = {
                        isDragging = false
                        previewNow(null)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        if (durationNow > 0) {
                            lastTargetMs = ((change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f) * durationNow).toLong()
                            previewNow(lastTargetMs)
                        }
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.25f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(animatedBuffered)
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.4f))
            )
            Box(
                Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(4.dp)
                    .background(accentColor)
            )
        }
        Box(
            modifier = Modifier
                .offset { IntOffset((trackWidthPx * animatedProgress - thumbPx / 2f).roundToInt(), 0) }
                .size(thumbSize)
                .clip(CircleShape)
                .background(accentColor)
        )
    }
}
