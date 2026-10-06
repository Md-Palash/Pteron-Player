package com.pteron.player.ui.audio

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pteron.player.playback.AudioPlayerController
import com.pteron.player.ui.common.TwoLineTitle
import com.pteron.player.ui.common.bouncyClickable
import com.pteron.player.ui.player.RepeatMode
import com.pteron.player.util.formatTimecode

/** Full-screen music player: big cover, seek bar and the transport controls. */
@Composable
fun NowPlayingScreen(
    controller: AudioPlayerController,
    viewModel: AudioViewModel,
    onBack: () -> Unit
) {
    val state by controller.state.collectAsState()
    val position by controller.positionMs.collectAsState()
    val audioState by viewModel.uiState.collectAsState()
    val prefs by viewModel.musicPrefs.collectAsState()
    val activity = LocalContext.current as? Activity

    val song = state.current
    // Nothing loaded (e.g. the queue was cleared): there is nothing to show here.
    LaunchedEffect(song == null) { if (song == null) onBack() }

    DisposableEffect(prefs.keepScreenOnInNowPlaying) {
        val window = activity?.window
        if (prefs.keepScreenOnInNowPlaying) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    if (song == null) return
    val isFavorite = remember(audioState.songs, song.id) { audioState.songs.firstOrNull { it.id == song.id }?.isFavorite ?: false }
    val scheme = MaterialTheme.colorScheme

    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val duration = state.durationMs.coerceAtLeast(1L)
    val shownPosition = if (dragging) dragValue.toLong() else position.coerceIn(0L, duration)

    Scaffold { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .background(scheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.padding(end = 8.dp).bouncyClickable(onClick = onBack)) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
                TwoLineTitle(subtitle = "PTERON PLAYER", title = "Now Playing")
            }
            Spacer(Modifier.height(20.dp))
            AudioArt(
                contentUri = song.contentUri,
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(32.dp)),
                iconSize = 64.dp
            )
            Spacer(Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(song.title, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        song.artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { viewModel.toggleFavorite(song.copy(isFavorite = isFavorite)) }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) scheme.primary else scheme.onSurfaceVariant
                    )
                }
            }
            Slider(
                value = shownPosition.toFloat().coerceIn(0f, duration.toFloat()),
                onValueChange = {
                    dragging = true
                    dragValue = it
                },
                onValueChangeFinished = {
                    controller.seekTo(dragValue.toLong())
                    dragging = false
                },
                valueRange = 0f..duration.toFloat()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTimecode(shownPosition), style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                Text(formatTimecode(duration), style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = controller::toggleShuffle) {
                    Icon(
                        Icons.Outlined.Shuffle, contentDescription = "Shuffle",
                        tint = if (state.shuffle) scheme.primary else scheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = controller::previous, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Outlined.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(32.dp))
                }
                Box(
                    modifier = Modifier.size(68.dp).clip(CircleShape).background(scheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = controller::togglePlayPause, modifier = Modifier.size(68.dp)) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            tint = scheme.onPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                IconButton(onClick = controller::next, enabled = state.hasNext, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Outlined.SkipNext, contentDescription = "Next", modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = controller::cycleRepeat) {
                    Icon(
                        imageVector = if (state.repeat == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Outlined.Repeat,
                        contentDescription = "Repeat: ${state.repeat.label}",
                        tint = if (state.repeat == RepeatMode.OFF) scheme.onSurfaceVariant else scheme.primary
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
