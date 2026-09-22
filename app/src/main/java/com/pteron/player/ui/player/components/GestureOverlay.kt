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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class SeekFlashSide { NONE, LEFT, RIGHT }

/**
 * Transparent gesture-capture layer placed above the video surface and below
 * the visible controls. Handles:
 *  - single tap: toggles control visibility
 *  - double tap left/right third: seek back/forward by the configured step, with a brief flash
 *  - vertical drag on the left half: screen brightness (reported via [onBrightnessChanged] for a HUD)
 *  - vertical drag on the right half: media volume (reported via [onVolumeChanged] for a HUD)
 *  - horizontal drag: scrub preview, committed on release
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
    currentPositionMs: Long,
    onToggleControls: () -> Unit,
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

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var scrubTargetMs by remember { mutableFloatStateOf(0f) }
    var isScrubbing by remember { mutableStateOf(false) }
    var dragStartedOnLeftHalf by remember { mutableStateOf(true) }

    // Anchored at the start of each vertical drag so brightness/volume track the finger by a pure
    // offset from where it began, rather than being recomputed from a system read on every pointer
    // move. That avoids two sources of jumpiness: a stale/-1 "use system default" brightness value
    // snapping to an arbitrary baseline, and a read-modify-write round trip drifting at the edges.
    var brightnessAnchor by remember { mutableFloatStateOf(0.5f) }
    var volumeIndexAnchor by remember { mutableIntStateOf(0) }
    var volumeMaxAnchor by remember { mutableIntStateOf(1) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(locked) {
                if (locked) return@pointerInput
                containerSize = size
                detectTapGestures(
                    onTap = { onToggleControls() },
                    onDoubleTap = { offset ->
                        val third = size.width / 3f
                        when {
                            offset.x < third -> {
                                onSeekBy(-seekStepMs)
                                onFlash(SeekFlashSide.LEFT)
                            }
                            offset.x > third * 2 -> {
                                onSeekBy(seekStepMs)
                                onFlash(SeekFlashSide.RIGHT)
                            }
                        }
                        scope.launch {
                            delay(450)
                            onFlash(SeekFlashSide.NONE)
                        }
                    }
                )
            }
            .pointerInput(locked, sensitivity, durationMs, currentPositionMs) {
                if (locked) return@pointerInput
                containerSize = size
                detectDragGestures(
                    onDragStart = { offset ->
                        scrubTargetMs = currentPositionMs.toFloat()
                        dragStartedOnLeftHalf = offset.x < containerSize.width / 2f
                        isScrubbing = false
                        brightnessAnchor = currentWindowBrightness(activity, context)
                        volumeIndexAnchor = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
                        volumeMaxAnchor = (audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 1).coerceAtLeast(1)
                    },
                    onDragEnd = {
                        if (isScrubbing) {
                            onScrubCommit(scrubTargetMs.toLong())
                            onScrubPreview(null)
                        }
                        isScrubbing = false
                    },
                    onDragCancel = {
                        if (isScrubbing) onScrubPreview(null)
                        isScrubbing = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val isHorizontalGesture = abs(dragAmount.x) > abs(dragAmount.y) * 1.3f
                        if (isHorizontalGesture && durationMs > 0) {
                            isScrubbing = true
                            val widthPx = containerSize.width.takeIf { it > 0 } ?: 1
                            // Full-width drag covers roughly 2 minutes of footage at 1.0x sensitivity.
                            val msPerPx = (120_000f / widthPx) * sensitivity
                            val deltaMs = dragAmount.x * msPerPx
                            scrubTargetMs = (scrubTargetMs + deltaMs).coerceIn(0f, durationMs.toFloat())
                            onScrubPreview(scrubTargetMs.toLong())
                        } else if (!isHorizontalGesture) {
                            val heightPx = containerSize.height.takeIf { it > 0 } ?: 1
                            val fraction = (dragAmount.y / heightPx) * sensitivity
                            if (dragStartedOnLeftHalf) {
                                brightnessAnchor = (brightnessAnchor - fraction).coerceIn(0.02f, 1f)
                                applyWindowBrightness(activity, brightnessAnchor)
                                onBrightnessChanged(brightnessAnchor)
                            } else {
                                val deltaSteps = -fraction * volumeMaxAnchor
                                val target = (volumeIndexAnchor + deltaSteps).toInt().coerceIn(0, volumeMaxAnchor)
                                if (target != volumeIndexAnchor) {
                                    volumeIndexAnchor = target
                                    audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
                                }
                                onVolumeChanged(target.toFloat() / volumeMaxAnchor.toFloat(), target, volumeMaxAnchor)
                            }
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
