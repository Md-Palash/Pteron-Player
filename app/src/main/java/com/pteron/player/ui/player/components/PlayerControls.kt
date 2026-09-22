package com.pteron.player.ui.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
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
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = 4.dp, vertical = 4.dp)
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

/** One control bar entry: an icon button plus what it needs to draw and act. */
private data class BarControl(
    val icon: ImageVector,
    val contentDescription: String,
    val tint: Color,
    val size: Dp = 22.dp,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

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
    onScrub: (Long) -> Unit,
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
    val playingTint = Color.White
    val dimTint = Color.White.copy(alpha = 0.35f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Scrubber(
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            bufferedPercentage = bufferedPercentage,
            accentColor = accentColor,
            onScrub = onScrub
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatTimecode(currentPositionMs), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
            Text(formatRemaining(durationMs - currentPositionMs), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
        }

        // Every control in one line, smaller and evenly spaced so shuffle through rotate all read
        // as one continuous transport strip. It scrolls only if a narrow screen can't fit every
        // icon at a comfortable tap size -- the play button stays the visual anchor in the middle.
        val leadControls = listOf(
            BarControl(Icons.Outlined.Shuffle, "Shuffle", if (shuffleEnabled) accentColor else dimTint) { onToggleShuffle() },
            BarControl(Icons.Outlined.SkipPrevious, "Previous", if (hasPrevious) playingTint else dimTint, enabled = hasPrevious) { onPrevious() },
            BarControl(Icons.Outlined.Replay, "Rewind $seekSeconds seconds", playingTint) { onSeekBy(-seekStepMs) }
        )
        val trailControls = listOf(
            BarControl(Icons.Outlined.FastForward, "Forward $seekSeconds seconds", playingTint) { onSeekBy(seekStepMs) },
            BarControl(Icons.Outlined.SkipNext, "Next", if (hasNext) playingTint else dimTint, enabled = hasNext) { onNext() },
            BarControl(
                if (repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Outlined.Repeat,
                "Repeat: ${repeatMode.label}",
                if (repeatMode == RepeatMode.OFF) dimTint else accentColor
            ) { onCycleRepeat() },
            BarControl(if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp, if (isMuted) "Unmute" else "Mute", playingTint) { onToggleMute() },
            BarControl(if (isLocked) Icons.Outlined.Lock else Icons.Outlined.LockOpen, "Lock controls", playingTint) { onToggleLock() },
            BarControl(Icons.Outlined.PictureInPictureAlt, "Picture in picture", playingTint) { onEnterPip() },
            BarControl(Icons.Outlined.AspectRatio, "Cycle aspect ratio", playingTint) { onCycleAspectRatio() },
            BarControl(Icons.Outlined.ScreenRotation, "Rotate", playingTint) { onToggleRotation() }
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(leadControls) { _, control -> BarIconButton(control) }
            item { TextIconChip(text = "${playbackSpeed}x", icon = Icons.Outlined.Speed, onClick = onOpenSpeedMenu) }
            item {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(accentColor)
                        .size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onPlayPause) {
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
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
            itemsIndexed(trailControls) { _, control -> BarIconButton(control) }
        }
    }
}

@Composable
private fun BarIconButton(control: BarControl) {
    IconButton(onClick = control.onClick, enabled = control.enabled, modifier = Modifier.size(36.dp)) {
        Icon(
            imageVector = control.icon,
            contentDescription = control.contentDescription,
            tint = control.tint,
            modifier = Modifier.size(control.size)
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
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
        Text(text, color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}

/** Effectively instant: used to keep the scrubber's fill glued to the finger while dragging. */
private val snapSpec = tween<Float>(durationMillis = 0)

@Composable
private fun Scrubber(
    currentPositionMs: Long,
    durationMs: Long,
    bufferedPercentage: Int,
    accentColor: Color,
    onScrub: (Long) -> Unit
) {
    val progress = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    val buffered = (bufferedPercentage / 100f).coerceIn(0f, 1f)

    // While the finger is actually on the bar, the fill must track it exactly -- animating here
    // would make it lag a beat behind the touch and feel unstable. The tween is only for the
    // normal once-a-second playback tick, where a glide reads as smooth rather than a jump.
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

    var trackWidthPx by remember { mutableFloatStateOf(1f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .pointerInput(durationMs, trackWidthPx) {
                detectTapGestures { offset ->
                    if (durationMs > 0 && trackWidthPx > 0) {
                        val fraction = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                        onScrub((fraction * durationMs).toLong())
                    }
                }
            }
            .pointerInput(durationMs, trackWidthPx) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false }
                ) { change, _ ->
                    if (durationMs > 0 && trackWidthPx > 0) {
                        val fraction = (change.position.x / trackWidthPx).coerceIn(0f, 1f)
                        onScrub((fraction * durationMs).toLong())
                    }
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.25f))
                .onGloballyPositioned { coordinates -> trackWidthPx = coordinates.size.width.toFloat() }
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
    }
}
