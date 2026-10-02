package com.pteron.player.ui.player.components

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

enum class SeekFlashSide { NONE, LEFT, RIGHT }

/** What a drag turned out to be. Decided once, a few dp into the drag, and then kept. */
private enum class DragAxis { UNDECIDED, HORIZONTAL, VERTICAL, IGNORED }

/**
 * Transparent gesture-capture layer placed above the video surface and below
 * the visible controls. Handles:
 *  - single tap: toggles control visibility (also while locked, so the unlock button can be shown)
 *  - double tap left/right third: seek back/forward by the configured step, with a brief flash
 *  - double tap middle third: play / pause
 *  - vertical drag on the left half: screen brightness (reported via [onBrightnessChanged] for a HUD)
 *  - vertical drag on the right half: media volume (reported via [onVolumeChanged] for a HUD)
 *  - horizontal drag: scrub preview, committed on release
 *
 * A drag is classified as horizontal or vertical once, near its start, and then stays that way,
 * so a slightly wobbly volume swipe can never turn into a scrub (and the other way round). Drags
 * that start within a thumb's width of the left/right edge are ignored so they never fight the
 * system back gesture. The handlers read the live values through [rememberUpdatedState], so the
 * once-per-second position tick never restarts (and so never interrupts) a drag in progress.
 *
 * All drag magnitudes are scaled by [sensitivity] (0.5x precise .. 2.0x fast),
 * persisted from Settings.
 */
@Composable
fun GestureOverlay(
    modifier: Modifier = Modifier,
    locked: Boolean,
    sensitivity: Float,
    seekStepMs: Long,
    durationMs: Long,
    /** Read only when a scrub starts, so the caller doesn't have to recompose every second. */
    positionProvider: () -> Long,
    onToggleControls: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekBy: (Long) -> Unit,
    onScrubPreview: (Long?) -> Unit,
    onScrubCommit: (Long) -> Unit,
    onFlash: (SeekFlashSide) -> Unit,
    onBrightnessChanged: (Float) -> Unit,
    onVolumeChanged: (Float, Int, Int) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val audioManager = remember { context.getSystemService(AudioManager::class.java) }
    val scope = rememberCoroutineScope()
    val flashJob = remember { arrayOfNulls<Job>(1) }

    val sensitivityNow by rememberUpdatedState(sensitivity)
    val seekStepNow by rememberUpdatedState(seekStepMs)
    val durationNow by rememberUpdatedState(durationMs)
    val positionNow by rememberUpdatedState(positionProvider)
    val toggleControls by rememberUpdatedState(onToggleControls)
    val togglePlayPause by rememberUpdatedState(onTogglePlayPause)
    val seekBy by rememberUpdatedState(onSeekBy)
    val scrubPreview by rememberUpdatedState(onScrubPreview)
    val scrubCommit by rememberUpdatedState(onScrubCommit)
    val flash by rememberUpdatedState(onFlash)
    val brightnessChanged by rememberUpdatedState(onBrightnessChanged)
    val volumeChanged by rememberUpdatedState(onVolumeChanged)

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(locked) {
                val onDoubleTapHandler: (Offset) -> Unit = { offset ->
                    val third = size.width / 3f
                    val side = when {
                        offset.x < third -> SeekFlashSide.LEFT
                        offset.x > third * 2 -> SeekFlashSide.RIGHT
                        else -> SeekFlashSide.NONE
                    }
                    when (side) {
                        SeekFlashSide.LEFT -> seekBy(-seekStepNow)
                        SeekFlashSide.RIGHT -> seekBy(seekStepNow)
                        SeekFlashSide.NONE -> togglePlayPause()
                    }
                    if (side != SeekFlashSide.NONE) {
                        flash(side)
                        // A new double tap restarts the hide timer instead of the old one cutting it short.
                        flashJob[0]?.cancel()
                        flashJob[0] = scope.launch {
                            delay(450)
                            flash(SeekFlashSide.NONE)
                        }
                    }
                }
                // While locked there is no double tap, so a single tap is reported immediately
                // (no wait for a possible second tap) to show the unlock button.
                detectTapGestures(
                    onTap = { toggleControls() },
                    onDoubleTap = if (locked) null else onDoubleTapHandler
                )
            }
            .pointerInput(locked) {
                if (locked) return@pointerInput

                val edgeGuardPx = 24.dp.toPx()
                val decideDistancePx = 6.dp.toPx()

                var axis = DragAxis.IGNORED
                var totalX = 0f
                var totalY = 0f
                var scrubbing = false
                var scrubTargetMs = 0f
                var startedOnLeftHalf = true
                var brightness = 0.5f
                var volumePosition = 0f
                var volumeIndex = 0
                var volumeMax = 1

                fun scrub(dx: Float) {
                    val duration = durationNow
                    if (duration <= 0L) return
                    scrubbing = true
                    // A full-width drag covers roughly 2 minutes of footage at 1.0x sensitivity.
                    val msPerPx = (120_000f / size.width.coerceAtLeast(1)) * sensitivityNow
                    scrubTargetMs = (scrubTargetMs + dx * msPerPx).coerceIn(0f, duration.toFloat())
                    scrubPreview(scrubTargetMs.toLong())
                }

                fun vertical(dy: Float) {
                    val fraction = (dy / size.height.coerceAtLeast(1)) * sensitivityNow
                    if (startedOnLeftHalf) {
                        brightness = (brightness - fraction).coerceIn(0.02f, 1f)
                        applyWindowBrightness(activity, brightness)
                        brightnessChanged(brightness)
                    } else {
                        // Kept as a float so a slow swipe accumulates instead of rounding away.
                        volumePosition = (volumePosition - fraction * volumeMax).coerceIn(0f, volumeMax.toFloat())
                        val target = volumePosition.roundToInt()
                        if (target != volumeIndex) {
                            volumeIndex = target
                            runCatching { audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0) }
                        }
                        volumeChanged(target.toFloat() / volumeMax.toFloat(), target, volumeMax)
                    }
                }

                detectDragGestures(
                    onDragStart = { offset ->
                        totalX = 0f
                        totalY = 0f
                        scrubbing = false
                        scrubTargetMs = positionNow().toFloat()
                        startedOnLeftHalf = offset.x < size.width / 2f
                        axis = if (offset.x < edgeGuardPx || offset.x > size.width - edgeGuardPx) {
                            DragAxis.IGNORED
                        } else {
                            DragAxis.UNDECIDED
                        }
                        if (axis != DragAxis.IGNORED) {
                            brightness = currentWindowBrightness(activity, context)
                            volumeMax = (audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 1).coerceAtLeast(1)
                            volumeIndex = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
                            volumePosition = volumeIndex.toFloat()
                        }
                    },
                    onDragEnd = {
                        if (scrubbing) {
                            scrubCommit(scrubTargetMs.toLong())
                            scrubPreview(null)
                        }
                        scrubbing = false
                        axis = DragAxis.IGNORED
                    },
                    onDragCancel = {
                        if (scrubbing) scrubPreview(null)
                        scrubbing = false
                        axis = DragAxis.IGNORED
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        when (axis) {
                            DragAxis.IGNORED -> Unit
                            DragAxis.UNDECIDED -> {
                                totalX += dragAmount.x
                                totalY += dragAmount.y
                                if (max(abs(totalX), abs(totalY)) >= decideDistancePx) {
                                    if (abs(totalX) > abs(totalY)) {
                                        axis = DragAxis.HORIZONTAL
                                        scrub(totalX)
                                    } else {
                                        axis = DragAxis.VERTICAL
                                        vertical(totalY)
                                    }
                                }
                            }
                            DragAxis.HORIZONTAL -> scrub(dragAmount.x)
                            DragAxis.VERTICAL -> vertical(dragAmount.y)
                        }
                    }
                )
            }
    )
}

/** The window's own brightness override, or the device's actual current system brightness if the
 *  window hasn't overridden it yet -- so the very first drag of a session starts from where the
 *  screen already visibly is, instead of snapping to an arbitrary 50% baseline. */
private fun currentWindowBrightness(activity: Activity?, context: Context): Float {
    val windowValue = activity?.window?.attributes?.screenBrightness ?: -1f
    if (windowValue >= 0f) return windowValue
    return runCatching {
        Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
    }.getOrDefault(128).coerceIn(0, 255) / 255f
}

private fun applyWindowBrightness(activity: Activity?, value: Float) {
    val window = activity?.window ?: return
    val params = window.attributes
    params.screenBrightness = value
    window.attributes = params
}
