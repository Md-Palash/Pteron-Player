package com.pteron.player.ui.audio

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pteron.player.data.model.AudioItem
import com.pteron.player.data.prefs.NowPlayingStyle
import com.pteron.player.playback.AudioPlayerController
import com.pteron.player.ui.common.TwoLineTitle
import com.pteron.player.ui.common.bouncyClickable
import com.pteron.player.ui.player.RepeatMode
import com.pteron.player.util.formatTimecode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Waves around the whole ring, and how tall they are. */
private const val RingWaves = 22
private val RingWaveAmplitude = 3.dp
private val RingStroke = 5.dp

/**
 * Full-screen music player. The screen is laid out in fixed bands so it looks the same on every
 * phone: the top 20% is empty (the header lives there), the middle 70% holds the card, the
 * title row, the seek bar and the controls, and the bottom 10% stays empty. Everything inside the
 * 70% band is sized from the room it gets, keeping the same proportions.
 */
@Composable
fun NowPlayingScreen(
    controller: AudioPlayerController,
    viewModel: AudioViewModel,
    onBack: () -> Unit
) {
    val state by controller.state.collectAsState()
    val audioState by viewModel.uiState.collectAsState()
    val prefs by viewModel.musicPrefs.collectAsState()
    val activity = LocalContext.current as? Activity
    val menu = remember { SongMenuState() }

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
    val context = LocalContext.current

    Scaffold { insets ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .background(scheme.background)
        ) {
            val screenHeight = maxHeight

            // Header: inside the empty top band.
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.padding(end = 8.dp).bouncyClickable(onClick = onBack)) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
                TwoLineTitle(subtitle = "PTERON PLAYER", title = "Now Playing")
            }

            // The 70% band: starts at 20% of the height, leaves 10% free underneath.
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = screenHeight * 0.20f)
                    .fillMaxWidth()
                    .height(screenHeight * 0.70f)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1) The card.
                BoxWithConstraints(modifier = Modifier.weight(0.56f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val side = min(maxWidth.value, maxHeight.value).dp
                    if (prefs.nowPlayingStyle == NowPlayingStyle.CIRCLE) {
                        CircularCard(song = song, controller = controller, side = side, isPlaying = state.isPlaying)
                    } else {
                        // Same footprint as the circular card (its art sits inside the ring's inset).
                        AudioArt(
                            contentUri = song.contentUri,
                            modifier = Modifier.size(side - RingInset * 2).clip(RoundedCornerShape(36.dp)),
                            iconSize = 64.dp
                        )
                    }
                }

                // 2) Title and artist on the left, share / favorite / add-to-playlist on the right.
                Row(
                    modifier = Modifier.weight(0.13f).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(song.title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            song.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SquareAction(Icons.Outlined.Share, "Share", false) { shareSong(context, song) }
                        SquareAction(
                            icon = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            description = "Favorite",
                            active = isFavorite
                        ) { viewModel.toggleFavorite(song.copy(isFavorite = isFavorite)) }
                        SquareAction(Icons.Outlined.PlaylistAdd, "Add to playlist", false) { menu.addToPlaylist = song }
                    }
                }

                // 3) Seek bar with the time labels.
                SeekBar(controller = controller, durationMs = state.durationMs, modifier = Modifier.weight(0.11f).fillMaxWidth())

                // 4) Controls: shuffle, previous, play/pause, next, repeat -- sizes step down from the middle.
                BoxWithConstraints(modifier = Modifier.weight(0.20f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val play = min(min(maxHeight.value * 0.95f, maxWidth.value / 4.3f), 88f).dp
                    val mid = play * 0.74f
                    val small = play * 0.56f
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RoundControl(
                            icon = Icons.Outlined.Shuffle, description = "Shuffle", size = small,
                            container = scheme.surfaceContainer,
                            tint = if (state.shuffle) scheme.primary else scheme.onSurfaceVariant,
                            onClick = controller::toggleShuffle
                        )
                        RoundControl(
                            icon = Icons.Rounded.FastRewind, description = "Previous", size = mid,
                            container = scheme.primary.copy(alpha = 0.16f), tint = scheme.primary,
                            onClick = controller::previous
                        )
                        RoundControl(
                            icon = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            description = if (state.isPlaying) "Pause" else "Play", size = play,
                            container = scheme.primary, tint = scheme.onPrimary,
                            onClick = controller::togglePlayPause
                        )
                        RoundControl(
                            icon = Icons.Rounded.FastForward, description = "Next", size = mid,
                            container = scheme.primary.copy(alpha = 0.16f), tint = scheme.primary,
                            enabled = state.hasNext, onClick = controller::next
                        )
                        RoundControl(
                            icon = if (state.repeat == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Outlined.Repeat,
                            description = "Repeat: ${state.repeat.label}", size = small,
                            container = scheme.surfaceContainer,
                            tint = if (state.repeat == RepeatMode.OFF) scheme.onSurfaceVariant else scheme.primary,
                            onClick = controller::cycleRepeat
                        )
                    }
                }
            }
        }
    }

    SongMenuHost(menu = menu, viewModel = viewModel, controller = controller, playlists = audioState.playlists)
}

// --- Circular card with the wavy progress ring ---------------------------------------------------

private val RingInset = 20.dp

@Composable
private fun CircularCard(song: AudioItem, controller: AudioPlayerController, side: Dp, isPlaying: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val positionState = controller.positionMs.collectAsState()
    val durationState = controller.state.collectAsState()

    // Smooths the 500 ms position ticks so the ring grows continuously. Read only while drawing.
    val target = run {
        val d = durationState.value.durationMs.coerceAtLeast(1L)
        (positionState.value.toFloat() / d.toFloat()).coerceIn(0f, 1f)
    }
    val progress = animateFloatAsState(target, tween(500, easing = LinearEasing), label = "ringProgress")

    // The waves drift only while music plays.
    val phase = remember { Animatable(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                phase.animateTo(phase.value + 2f * PI.toFloat(), tween(2600, easing = LinearEasing))
            }
        }
    }

    val track = scheme.outlineVariant.copy(alpha = 0.6f)
    val accent = scheme.primary

    Box(modifier = Modifier.size(side), contentAlignment = Alignment.Center) {
        AudioArt(
            contentUri = song.contentUri,
            modifier = Modifier.size(side - RingInset * 2).clip(CircleShape),
            iconSize = 64.dp
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = RingStroke.toPx()
            val amp = RingWaveAmplitude.toPx()
            val radius = size.minDimension / 2f - amp - strokePx / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            drawCircle(color = track, radius = radius, center = center, style = Stroke(width = strokePx * 0.6f))

            val p = progress.value
            if (p > 0.002f) {
                val sweep = 2f * PI.toFloat() * p
                val steps = max(8, (p * 360f).toInt())
                val path = Path()
                var endX = 0f
                var endY = 0f
                for (i in 0..steps) {
                    val t = sweep * i / steps
                    val angle = -PI.toFloat() / 2f + t
                    // The wave fades in over the first bit of the arc so the start is a clean point.
                    val r = radius + amp * sin(RingWaves * t + phase.value)
                    val x = center.x + r * cos(angle)
                    val y = center.y + r * sin(angle)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    endX = x
                    endY = y
                }
                drawPath(
                    path = path,
                    color = accent,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawCircle(color = accent, radius = strokePx * 1.2f, center = Offset(endX, endY))
            }
        }
    }
}

// --- Seek bar --------------------------------------------------------------------------------------

@Composable
private fun SeekBar(controller: AudioPlayerController, durationMs: Long, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val position: State<Long> = controller.positionMs.collectAsState()
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val duration = durationMs.coerceAtLeast(1L)
    val shown = if (dragging) dragValue.toLong() else position.value.coerceIn(0L, duration)

    Column(modifier = modifier, verticalArrangement = Arrangement.Center) {
        Slider(
            value = shown.toFloat().coerceIn(0f, duration.toFloat()),
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
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTimecode(shown), style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            Text(formatTimecode(duration), style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
        }
    }
}

// --- Buttons ---------------------------------------------------------------------------------------

/** Square button with rounded corners (share / favorite / add to playlist). */
@Composable
private fun SquareAction(icon: ImageVector, description: String, active: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) scheme.primary.copy(alpha = 0.16f) else scheme.surfaceContainer)
            .bouncyClickable(pressedScale = 0.92f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (active) scheme.primary else scheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
    }
}

/** Round transport button; the icon is half the button's diameter. */
@Composable
private fun RoundControl(
    icon: ImageVector,
    description: String,
    size: Dp,
    container: Color,
    tint: Color,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(container)
            .bouncyClickable(enabled = enabled, pressedScale = 0.92f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (enabled) tint else tint.copy(alpha = 0.35f),
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

private fun shareSong(context: Context, song: AudioItem) {
    runCatching {
        val uri = Uri.parse(song.contentUri)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, song.title)
            clipData = ClipData.newRawUri(song.title, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Share ${song.title}").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
