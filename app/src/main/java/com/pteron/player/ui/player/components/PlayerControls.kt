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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pteron.player.util.formatRemaining
import com.pteron.player.util.formatTimecode

@Composable
fun PlayerTopBar(
    title: String,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenAudioTracks: () -> Unit,
    onOpenSubtitleTracks: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Exit player", tint = Color.White)
        }
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
        )
        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "Favorite",
                tint = Color.White
            )
        }
        IconButton(onClick = onOpenAudioTracks) {
            Icon(Icons.Filled.Speed, contentDescription = "Audio track", tint = Color.White)
        }
        IconButton(onClick = onOpenSubtitleTracks) {
            Icon(Icons.Filled.Subtitles, contentDescription = "Subtitles", tint = Color.White)
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
    accentColor: Color,
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
    modifier: Modifier = Modifier
) {
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
            IconButton(onClick = onPrevious, enabled = hasPrevious) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = if (hasPrevious) Color.White else Color.White.copy(alpha = 0.3f))
            }
            IconButton(onClick = { onSeekBy(-10_000L) }) {
                Icon(Icons.Filled.Replay10, contentDescription = "Rewind 10 seconds", tint = Color.White)
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
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            IconButton(onClick = { onSeekBy(10_000L) }) {
                Icon(Icons.Filled.Forward10, contentDescription = "Forward 10 seconds", tint = Color.White)
            }
            IconButton(onClick = onNext, enabled = hasNext) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = if (hasNext) Color.White else Color.White.copy(alpha = 0.3f))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextIconChip(text = "${playbackSpeed}x", icon = Icons.Filled.Speed, onClick = onOpenSpeedMenu)
            IconButton(onClick = onToggleLock) {
                Icon(
                    imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    contentDescription = "Lock controls",
                    tint = Color.White
                )
            }
            IconButton(onClick = onEnterPip) {
                Icon(Icons.Filled.PictureInPictureAlt, contentDescription = "Picture in picture", tint = Color.White)
            }
            IconButton(onClick = onCycleAspectRatio) {
                Icon(Icons.Filled.AspectRatio, contentDescription = "Aspect ratio", tint = Color.White)
            }
            IconButton(onClick = onToggleRotation) {
                Icon(Icons.Filled.ScreenRotation, contentDescription = "Rotate", tint = Color.White)
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
            .then(androidx.compose.ui.input.pointer.pointerInput(Unit) { detectTapGestures { onClick() } })
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

    var trackWidthPx by androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(1f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .pointerInputScrub(durationMs, trackWidthPx, onScrub),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.25f))
                .onGloballyPositionedWidth { trackWidthPx = it }
        ) {
            Box(
                Modifier
                    .fillMaxWidth(buffered)
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.4f))
            )
            Box(
                Modifier
                    .fillMaxWidth(progress)
                    .height(4.dp)
                    .background(accentColor)
            )
        }
    }
}

private fun Modifier.onGloballyPositionedWidth(onWidth: (Float) -> Unit): Modifier =
    this.then(
        androidx.compose.ui.layout.onGloballyPositioned { coordinates ->
            onWidth(coordinates.size.width.toFloat())
        }
    )

private fun Modifier.pointerInputScrub(durationMs: Long, trackWidthPx: Float, onScrub: (Long) -> Unit): Modifier =
    this.then(
        androidx.compose.ui.input.pointer.pointerInput(durationMs, trackWidthPx) {
            detectTapGestures { offset ->
                if (durationMs > 0 && trackWidthPx > 0) {
                    val fraction = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                    onScrub((fraction * durationMs).toLong())
                }
            }
        }
    ).then(
        androidx.compose.ui.input.pointer.pointerInput(durationMs, trackWidthPx) {
            detectDragGestures { change, _ ->
                if (durationMs > 0 && trackWidthPx > 0) {
                    val fraction = (change.position.x / trackWidthPx).coerceIn(0f, 1f)
                    onScrub((fraction * durationMs).toLong())
                }
            }
        }
    )
