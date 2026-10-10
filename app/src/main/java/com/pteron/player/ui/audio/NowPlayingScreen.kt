package com.pteron.player.ui.audio

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pteron.player.data.model.AudioItem
import com.pteron.player.data.prefs.NowPlayingStyle
import com.pteron.player.playback.AudioPlayerController
import com.pteron.player.playback.AudioPlayerState
import com.pteron.player.theme.LocalThemeColors
import com.pteron.player.ui.common.bouncyClickable
import com.pteron.player.ui.player.RepeatMode
import com.pteron.player.util.formatTimecode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private val White70 = Color.White.copy(alpha = 0.70f)
private val HeartTint = Color(0xFFFF8A9B)

/**
 * Full-screen music player. The whole screen is one card whose background color is taken from the
 * song's cover, so the picture melts into the rest of the screen.
 *
 *  - Square style: the cover fills the top 60% and fades into the background; progress bar and
 *    controls sit in the bottom 40%.
 *  - Round style: header, time, a flower-shaped cover with a progress line around it, title,
 *    seek bar and controls.
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
    val context = LocalContext.current
    val activity = context as? Activity
    val menu = remember { SongMenuState() }
    val themeIsDark = LocalThemeColors.current.isDark

    val song = state.current
    // Nothing loaded (e.g. the queue was cleared): there is nothing to show here.
    LaunchedEffect(song == null) { if (song == null) onBack() }

    DisposableEffect(prefs.keepScreenOnInNowPlaying) {
        val window = activity?.window
        if (prefs.keepScreenOnInNowPlaying) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    // The screen is always dark, so the status / navigation bar icons must be light here,
    // whatever theme the rest of the app uses.
    DisposableEffect(themeIsDark) {
        val window = activity?.window
        val bars = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        bars?.isAppearanceLightStatusBars = false
        bars?.isAppearanceLightNavigationBars = false
        onDispose {
            bars?.isAppearanceLightStatusBars = !themeIsDark
            bars?.isAppearanceLightNavigationBars = !themeIsDark
        }
    }

    if (song == null) return

    val scheme = MaterialTheme.colorScheme
    val fallbackTint = remember(scheme.primary) { deepTint(scheme.primary) }
    val tint by animateColorAsState(
        targetValue = rememberArtTint(song.contentUri, fallbackTint),
        animationSpec = tween(500),
        label = "nowPlayingTint"
    )
    val isFavorite = remember(audioState.songs, song.id) {
        audioState.songs.firstOrNull { it.id == song.id }?.isFavorite ?: false
    }
    val onFavorite: () -> Unit = { viewModel.toggleFavorite(song.copy(isFavorite = isFavorite)) }
    val onShare: () -> Unit = { shareSong(context, song) }
    val onAddToPlaylist: () -> Unit = { menu.addToPlaylist = song }

    Box(modifier = Modifier.fillMaxSize().background(tint)) {
        if (prefs.nowPlayingStyle == NowPlayingStyle.SQUARE) {
            SquareLayout(song, state, controller, tint, isFavorite, onBack, onFavorite, onShare, onAddToPlaylist)
        } else {
            RoundLayout(song, state, controller, tint, isFavorite, onBack, onFavorite, onShare, onAddToPlaylist)
        }
    }

    SongMenuHost(menu = menu, viewModel = viewModel, controller = controller, playlists = audioState.playlists)
}

// --- Square style: cover on the top 60%, controls on the bottom 40% -----------------------------

@Composable
private fun SquareLayout(
    song: AudioItem,
    state: AudioPlayerState,
    controller: AudioPlayerController,
    tint: Color,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().weight(0.6f)) {
            AudioArt(song.contentUri, Modifier.fillMaxSize(), iconSize = 96.dp)
            // Keeps the header buttons readable on a bright cover.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent)))
            )
            // The blend: the cover fades into the screen's own color, so there is no edge.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, tint.copy(alpha = 0.6f), tint)))
            )
            NpHeader(isFavorite, onBack, onFavorite, Modifier.align(Alignment.TopCenter).statusBarsPadding())
        }
        Column(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(song.title, style = MaterialTheme.typography.titleLarge, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song.artist, style = MaterialTheme.typography.bodyMedium, color = White70, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                NpSquareAction(Icons.Outlined.Share, "Share", onShare)
                NpSquareAction(Icons.Outlined.PlaylistAdd, "Add to playlist", onAddToPlaylist)
            }
            NpSeekBar(controller, state.durationMs, Modifier.fillMaxWidth())
            NpTransport(state, controller, tint, Modifier.fillMaxWidth())
        }
    }
}

// --- Round style: flower-shaped cover with a progress line --------------------------------------

@Composable
private fun RoundLayout(
    song: AudioItem,
    state: AudioPlayerState,
    controller: AudioPlayerController,
    tint: Color,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NpHeader(isFavorite, onBack, onFavorite)
        NpTimeLabel(controller, state.durationMs)

        BoxWithConstraints(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            val side = min(maxWidth.value, maxHeight.value).dp
            FlowerCard(song, controller, side, state.durationMs)
        }

        Column(modifier = Modifier.padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                song.title, style = MaterialTheme.typography.titleLarge, color = Color.White,
                textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                song.artist, style = MaterialTheme.typography.bodyMedium, color = White70,
                textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        NpSeekBar(controller, state.durationMs, Modifier.fillMaxWidth().padding(horizontal = 24.dp))
        NpTransport(state, controller, tint, Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            NpSquareAction(Icons.Outlined.Share, "Share", onShare)
            NpSquareAction(Icons.Outlined.PlaylistAdd, "Add to playlist", onAddToPlaylist)
        }
    }
}

/** Petals around the cover and its progress line. */
private const val Lobes = 8
private val RingInset = 22.dp

/** Points of the flower outline from [startAngle] over [sweep] radians (0 deg = right, clockwise). */
private fun flowerPath(cx: Float, cy: Float, base: Float, amp: Float, startAngle: Float, sweep: Float, steps: Int): Path {
    val path = Path()
    for (i in 0..steps) {
        val t = startAngle + sweep * i / steps
        val r = base + amp * cos(Lobes * t)
        val x = cx + r * cos(t)
        val y = cy + r * sin(t)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    return path
}

/** The cover is clipped to this flower. */
private val FlowerShape = GenericShape { size, _ ->
    val half = size.minDimension / 2f
    addPath(flowerPath(size.width / 2f, size.height / 2f, half * 0.94f, half * 0.06f, -PI.toFloat() / 2f, 2f * PI.toFloat(), 240))
    close()
}

@Composable
private fun FlowerCard(song: AudioItem, controller: AudioPlayerController, side: Dp, durationMs: Long) {
    val position = controller.positionMs.collectAsStateWithLifecycle()
    val target = (position.value.toFloat() / durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
    // Smooths the 500 ms position ticks so the line grows continuously.
    val progress = animateFloatAsState(target, tween(500, easing = LinearEasing), label = "ringProgress")

    Box(modifier = Modifier.size(side), contentAlignment = Alignment.Center) {
        AudioArt(
            contentUri = song.contentUri,
            modifier = Modifier.size(side - RingInset * 2).clip(FlowerShape),
            iconSize = 64.dp
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val half = size.minDimension / 2f
            val amp = 5.dp.toPx()
            val base = half - amp - 2.dp.toPx()
            val cx = size.width / 2f
            val cy = size.height / 2f
            val start = -PI.toFloat() / 2f
            val full = 2f * PI.toFloat()

            drawPath(
                flowerPath(cx, cy, base, amp, start, full, 240),
                color = Color.White.copy(alpha = 0.28f),
                style = Stroke(width = 1.5.dp.toPx())
            )
            val p = progress.value
            if (p > 0.002f) {
                drawPath(
                    flowerPath(cx, cy, base, amp, start, full * p, max(8, (p * 240).toInt())),
                    color = Color.White,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
            val t = start + full * p
            val r = base + amp * cos(Lobes * t)
            drawCircle(Color.White, radius = 6.dp.toPx(), center = Offset(cx + r * cos(t), cy + r * sin(t)))
        }
    }
}

// --- Shared pieces -------------------------------------------------------------------------------

@Composable
private fun NpHeader(isFavorite: Boolean, onBack: () -> Unit, onFavorite: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NpCircleButton(Icons.AutoMirrored.Outlined.ArrowBack, "Back", false, onBack)
        Text(
            "Now Playing",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        NpCircleButton(
            icon = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            description = "Favorite",
            active = isFavorite,
            onClick = onFavorite
        )
    }
}

@Composable
private fun NpCircleButton(icon: ImageVector, description: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .bouncyClickable(pressedScale = 0.92f, onClick = onClick)
            .background(Color.White.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = if (active) HeartTint else Color.White, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun NpSquareAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .bouncyClickable(pressedScale = 0.92f, onClick = onClick)
            .background(Color.White.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

/** "01:23 | 03:40" under the header of the round style. */
@Composable
private fun NpTimeLabel(controller: AudioPlayerController, durationMs: Long) {
    val position = controller.positionMs.collectAsStateWithLifecycle()
    Row(
        modifier = Modifier.padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(formatTimecode(position.value), style = MaterialTheme.typography.titleMedium, color = Color.White)
        Text("  |  ", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.4f))
        Text(formatTimecode(durationMs), style = MaterialTheme.typography.titleMedium, color = White70)
    }
}

@Composable
private fun NpSeekBar(controller: AudioPlayerController, durationMs: Long, modifier: Modifier = Modifier) {
    val position: State<Long> = controller.positionMs.collectAsStateWithLifecycle()
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
            valueRange = 0f..duration.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
            )
        )
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTimecode(shown), style = MaterialTheme.typography.labelMedium, color = White70)
            Text(formatTimecode(duration), style = MaterialTheme.typography.labelMedium, color = White70)
        }
    }
}

/** Shuffle, previous, play / pause, next, repeat. The play button is white with the screen color as its icon. */
@Composable
private fun NpTransport(state: AudioPlayerState, controller: AudioPlayerController, tint: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NpRoundControl(
            Icons.Outlined.Shuffle, "Shuffle", 42.dp,
            container = Color.White.copy(alpha = if (state.shuffle) 0.30f else 0.10f),
            tint = if (state.shuffle) Color.White else White70,
            onClick = controller::toggleShuffle
        )
        NpRoundControl(
            Icons.Rounded.FastRewind, "Previous", 52.dp,
            container = Color.White.copy(alpha = 0.16f), tint = Color.White, onClick = controller::previous
        )
        NpRoundControl(
            if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            if (state.isPlaying) "Pause" else "Play", 72.dp,
            container = Color.White, tint = tint, onClick = controller::togglePlayPause
        )
        NpRoundControl(
            Icons.Rounded.FastForward, "Next", 52.dp,
            container = Color.White.copy(alpha = 0.16f), tint = Color.White,
            enabled = state.hasNext, onClick = controller::next
        )
        NpRoundControl(
            if (state.repeat == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Outlined.Repeat,
            "Repeat: ${state.repeat.label}", 42.dp,
            container = Color.White.copy(alpha = if (state.repeat == RepeatMode.OFF) 0.10f else 0.30f),
            tint = if (state.repeat == RepeatMode.OFF) White70 else Color.White,
            onClick = controller::cycleRepeat
        )
    }
}

@Composable
private fun NpRoundControl(
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
            .bouncyClickable(enabled = enabled, pressedScale = 0.92f, onClick = onClick)
            .background(container),
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
