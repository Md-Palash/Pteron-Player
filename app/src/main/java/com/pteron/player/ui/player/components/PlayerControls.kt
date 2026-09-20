package com.pteron.player.ui.player.components

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextOverflow
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
            if (isHardwareDecoder != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isHardwareDecoder) Color(0xFFD26938).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(if (isHardwareDecoder) "HW+" else "SW", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
                androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
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

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleShuffle) {
                Icon(
                    Icons.Outlined.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (shuffleEnabled) accentColor else Color.White.copy(alpha = 0.6f)
                )
            }
            IconButton(onClick = onPrevious, enabled = hasPrevious) {
                Icon(Icons.Outlined.SkipPrevious, contentDescription = "Previous", tint = if (hasPrevious) Color.White else Color.White.copy(alpha = 0.3f))
            }
            IconButton(onClick = { onSeekBy(-seekStepMs) }) {
                Icon(Icons.Outlined.Replay, contentDescription = "Rewind $seekSeconds seconds", tint = Color.White)
            }
            Box(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(accentColor)
                    .size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onPlayPause) {
                    androidx.compose.animation.Crossfade(
                        targetState = isPlaying,
                        animationSpec = androidx.compose.animation.core.tween(150),
                        label = "playPauseIcon"
                    ) { playing ->
                        Icon(
                            imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (playing) "Pause" else "Play",
                            // Readable on any accent: white on deep accents, dark on bright ones.
                            tint = if (accentColor.luminance() > 0.4f) Color.Black else Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
            IconButton(onClick = { onSeekBy(seekStepMs) }) {
                Icon(Icons.Outlined.FastForward, contentDescription = "Forward $seekSeconds seconds", tint = Color.White)
            }
            IconButton(onClick = onNext, enabled = hasNext) {
                Icon(Icons.Outlined.SkipNext, contentDescription = "Next", tint = if (hasNext) Color.White else Color.White.copy(alpha = 0.3f))
            }
            IconButton(onClick = onCycleRepeat) {
                Icon(
                    imageVector = if (repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Outlined.Repeat,
                    contentDescription = "Repeat: ${repeatMode.label}",
                    tint = if (repeatMode == RepeatMode.OFF) Color.White.copy(alpha = 0.6f) else accentColor
                )
            }
        }

        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item { TextIconChip(text = "${playbackSpeed}x", icon = Icons.Outlined.Speed, onClick = onOpenSpeedMenu) }
            item {
                IconButton(onClick = onToggleMute) {
                    Icon(
                        imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        tint = Color.White
                    )
                }
            }
            item {
                IconButton(onClick = onToggleLock) {
                    Icon(
                        imageVector = if (isLocked) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                        contentDescription = "Lock controls",
                        tint = Color.White
                    )
                }
            }
            item {
                IconButton(onClick = onEnterPip) {
                    Icon(Icons.Outlined.PictureInPictureAlt, contentDescription = "Picture in picture", tint = Color.White)
                }
            }
            item {
                IconButton(onClick = onCycleAspectRatio) {
                    Icon(Icons.Outlined.AspectRatio, contentDescription = "Cycle aspect ratio", tint = Color.White)
                }
            }
            item {
                IconButton(onClick = onToggleRotation) {
                    Icon(Icons.Outlined.ScreenRotation, contentDescription = "Rotate", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun TextIconChip(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.15f))
            .pointerInput(Unit) { detectTapGestures { onClick() } }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
        Text(text, color = Color.White, style = MaterialTheme.typography.labelMedium)
    }
}

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
    // Animated rather than snapping instantly, so the played/buffered fill glides
    // forward smoothly each tick instead of jumping in visible steps.
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = progress,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 250),
        label = "scrubberProgress"
    )
    val animatedBuffered by androidx.compose.animation.core.animateFloatAsState(
        targetValue = buffered,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 250),
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
                detectDragGestures { change, _ ->
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
