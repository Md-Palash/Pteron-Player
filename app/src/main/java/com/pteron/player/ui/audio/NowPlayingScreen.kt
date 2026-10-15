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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
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
import androidx.compose.ui.graphics.Shape
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
import com.pteron.player.theme.readableOn
import com.pteron.player.ui.common.bouncyClickable
import com.pteron.player.ui.player.RepeatMode
import com.pteron.player.util.formatTimecode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Colour of text and icons on the cover-coloured background: white on dark, near-black on light. */
private val LocalNpContent = compositionLocalOf { Color.White }
private val NpContent: Color @Composable get() = LocalNpContent.current
private val NpContent70: Color @Composable get() = LocalNpContent.current.copy(alpha = 0.70f)
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
    val contentColor = readableOn(tint)
    val lightBackground = contentColor != Color.White
    // Dark status/navigation icons on a light background, light ones on a dark one.
    DisposableEffect(lightBackground) {
        val window = activity?.window
        val bars = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        bars?.isAppearanceLightStatusBars = lightBackground
        bars?.isAppearanceLightNavigationBars = lightBackground
        onDispose { }
    }
    val isFavorite = remember(audioState.songs, song.id) {
        audioState.songs.firstOrNull { it.id == song.id }?.isFavorite ?: false
    }
    val onFavorite: () -> Unit = { viewModel.toggleFavorite(song.copy(isFavorite = isFavorite)) }
    val onShare: () -> Unit = { shareSong(context, song) }
    val onAddToPlaylist: () -> Unit = { menu.addToPlaylist = song }

    CompositionLocalProvider(LocalNpContent provides contentColor) {
        Box(modifier = Modifier.fillMaxSize().background(tint)) {
            if (prefs.nowPlayingStyle == NowPlayingStyle.SQUARE) {
                SquareLayout(song, state, controller, tint, isFavorite, onBack, onFavorite, onShare, onAddToPlaylist)
            } else {
                RoundLayout(song, state, controller, tint, isFavorite, onBack, onFavorite, onShare, onAddToPlaylist)
            }
        }
    }

    SongMenuHost(menu = menu, viewModel = viewModel, controller = controller, playlists = audioState.playlists)
}

// --- Square style: a lifted square cover, controls underneath -----------------------------------

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
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(bottom = 64.dp)
    ) {
        NpHeader(isFavorite, onBack, onFavorite)

        // The cover is the biggest square that fits the free space (with a margin), so it can
        // never run off the screen on any phone or orientation.
        BoxWithConstraints(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            val side = min(maxWidth.value, maxHeight.value).dp
            val shape = RoundedCornerShape(32.dp)
            Box(modifier = Modifier.size(side).pillowLift(shape, 24.dp).clip(shape)) {
                AudioArt(song.contentUri, Modifier.fillMaxSize(), iconSize = 96.dp)
                PillowSheen(shape)
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(song.title, style = MaterialTheme.typography.titleLarge, color = NpContent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song.artist, style = MaterialTheme.typography.bodyMedium, color = NpContent70, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                NpSquareAction(Icons.Outlined.Share, "Share", onShare)
                NpSquareAction(Icons.Outlined.PlaylistAdd, "Add to playlist", onAddToPlaylist)
            }
            NpSeekBar(controller, state.durationMs, Modifier.fillMaxWidth())
            NpTransport(state, controller, tint, Modifier.fillMaxWidth(), playSquare = true)
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
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(bottom = 40.dp),
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
                song.title, style = MaterialTheme.typography.titleLarge, color = NpContent,
                textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                song.artist, style = MaterialTheme.typography.bodyMedium, color = NpContent70,
                textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        NpSeekBar(controller, state.durationMs, Modifier.fillMaxWidth().padding(horizontal = 24.dp))
        NpTransport(state, controller, tint, Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), playSquare = false)
        Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            NpSquareAction(Icons.Outlined.Share, "Share", onShare)
            NpSquareAction(Icons.Outlined.PlaylistAdd, "Add to playlist", onAddToPlaylist)
        }
    }
}

/** Petals around the cover and its progress line. */
private const val Lobes = 8

/** The cover's flower: radius = half * (CoverBase + CoverAmp * cos(Lobes * angle)). */
private const val CoverBase = 0.94f
private const val CoverAmp = 0.06f

/** Distance between the cover's edge and the progress line, the same all the way around. */
private val RingGap = 10.dp

/** Space kept free around the cover: the gap plus room for the line and the progress dot. */
private val RingInset = RingGap + 9.dp

/**
 * A point of the flower outline at [angle] (0 = right, clockwise). [offset] pushes the point
 * outwards along the outline's own normal, so the offset curve keeps the same distance from the
 * cover everywhere (a plain radial offset would be narrower on the slopes of the petals).
 */
private fun flowerPoint(cx: Float, cy: Float, base: Float, amp: Float, offset: Float, angle: Float): Offset {
    val r = base + amp * cos(Lobes * angle)
    var x = cx + r * cos(angle)
    var y = cy + r * sin(angle)
    if (offset != 0f) {
        val dr = -amp * Lobes * sin(Lobes * angle)
        val dx = dr * cos(angle) - r * sin(angle)
        val dy = dr * sin(angle) + r * cos(angle)
        val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1e-3f)
        x += offset * dy / len
        y -= offset * dx / len
    }
    return Offset(x, y)
}

private fun flowerPath(
    cx: Float, cy: Float, base: Float, amp: Float, offset: Float,
    startAngle: Float, sweep: Float, steps: Int
): Path {
    val path = Path()
    for (i in 0..steps) {
        val p = flowerPoint(cx, cy, base, amp, offset, startAngle + sweep * i / steps)
        if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
    }
    return path
}

/** The cover is clipped to this flower. */
private val FlowerShape = GenericShape { size, _ ->
    val half = size.minDimension / 2f
    addPath(
        flowerPath(
            size.width / 2f, size.height / 2f, half * CoverBase, half * CoverAmp, 0f,
            -PI.toFloat() / 2f, 2f * PI.toFloat(), 240
        )
    )
    close()
}

@Composable
private fun FlowerCard(song: AudioItem, controller: AudioPlayerController, side: Dp, durationMs: Long) {
    val content = NpContent
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
            // Exactly the cover's flower, pushed outwards by RingGap.
            val coverHalf = size.minDimension / 2f - RingInset.toPx()
            val base = coverHalf * CoverBase
            val amp = coverHalf * CoverAmp
            val gap = RingGap.toPx()
            val cx = size.width / 2f
            val cy = size.height / 2f
            val start = -PI.toFloat() / 2f
            val full = 2f * PI.toFloat()

            drawPath(
                flowerPath(cx, cy, base, amp, gap, start, full, 240),
                color = content.copy(alpha = 0.28f),
                style = Stroke(width = 1.5.dp.toPx())
            )
            val p = progress.value
            if (p > 0.002f) {
                drawPath(
                    flowerPath(cx, cy, base, amp, gap, start, full * p, max(8, (p * 240).toInt())),
                    color = content,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
            drawCircle(content, radius = 6.dp.toPx(), center = flowerPoint(cx, cy, base, amp, gap, start + full * p))
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
            color = NpContent,
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
            .background(NpContent.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = if (active) HeartTint else NpContent, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun NpSquareAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .bouncyClickable(pressedScale = 0.92f, onClick = onClick)
            .background(NpContent.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = NpContent, modifier = Modifier.size(22.dp))
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
        Text(formatTimecode(position.value), style = MaterialTheme.typography.titleMedium, color = NpContent)
        Text("  |  ", style = MaterialTheme.typography.bodyMedium, color = NpContent.copy(alpha = 0.4f))
        Text(formatTimecode(durationMs), style = MaterialTheme.typography.titleMedium, color = NpContent70)
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
                thumbColor = NpContent,
                activeTrackColor = NpContent,
                inactiveTrackColor = NpContent.copy(alpha = 0.25f)
            )
        )
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTimecode(shown), style = MaterialTheme.typography.labelMedium, color = NpContent70)
            Text(formatTimecode(duration), style = MaterialTheme.typography.labelMedium, color = NpContent70)
        }
    }
}

/**
 * Shuffle, previous, play / pause, next, repeat. The play button takes the content color with the
 * screen color as its icon. In the round card the play button is round and the other four are
 * rounded squares; in the square card it is the other way round.
 */
@Composable
private fun NpTransport(
    state: AudioPlayerState,
    controller: AudioPlayerController,
    tint: Color,
    modifier: Modifier = Modifier,
    playSquare: Boolean
) {
    val small = if (playSquare) 46.dp else 44.dp
    val medium = if (playSquare) 58.dp else 56.dp
    val big = if (playSquare) 84.dp else 80.dp
    val playShape: Shape = if (playSquare) RoundedCornerShape(26.dp) else CircleShape
    fun other(size: Dp): Shape = if (playSquare) CircleShape else RoundedCornerShape(size * 0.3f)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NpRoundControl(
            Icons.Outlined.Shuffle, "Shuffle", small,
            container = NpContent.copy(alpha = if (state.shuffle) 0.30f else 0.10f),
            tint = if (state.shuffle) NpContent else NpContent70,
            shape = other(small),
            onClick = controller::toggleShuffle
        )
        NpRoundControl(
            Icons.Rounded.FastRewind, "Previous", medium,
            container = NpContent.copy(alpha = 0.16f), tint = NpContent, shape = other(medium),
            onClick = controller::previous
        )
        NpRoundControl(
            if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            if (state.isPlaying) "Pause" else "Play", big,
            container = NpContent, tint = tint, shape = playShape, onClick = controller::togglePlayPause
        )
        NpRoundControl(
            Icons.Rounded.FastForward, "Next", medium,
            container = NpContent.copy(alpha = 0.16f), tint = NpContent, shape = other(medium),
            enabled = state.hasNext, onClick = controller::next
        )
        NpRoundControl(
            if (state.repeat == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Outlined.Repeat,
            "Repeat: ${state.repeat.label}", small,
            container = NpContent.copy(alpha = if (state.repeat == RepeatMode.OFF) 0.10f else 0.30f),
            tint = if (state.repeat == RepeatMode.OFF) NpContent70 else NpContent,
            shape = other(small),
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
    shape: Shape = CircleShape,
    enabled: Boolean = true
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
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
