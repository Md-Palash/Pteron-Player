package com.pteron.player.ui.player.components

import android.app.Activity
import android.media.AudioManager
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
 *  - double tap left/right third: -10s / +10s seek with a brief flash
 *  - vertical drag on the left half: screen brightness
 *  - vertical drag on the right half: media volume
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
    durationMs: Long,
    currentPositionMs: Long,
    onToggleControls: () -> Unit,
    onSeekBy: (Long) -> Unit,
    onScrubPreview: (Long?) -> Unit,
    onScrubCommit: (Long) -> Unit,
    onFlash: (SeekFlashSide) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val audioManager = remember { context.getSystemService(AudioManager::class.java) }
    val scope = rememberCoroutineScope()

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var scrubTargetMs by remember { mutableFloatStateOf(0f) }
    var isScrubbing by remember { mutableStateOf(false) }
    var dragStartedOnLeftHalf by remember { mutableStateOf(true) }

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
                                onSeekBy(-10_000L)
                                onFlash(SeekFlashSide.LEFT)
                            }
                            offset.x > third * 2 -> {
                                onSeekBy(10_000L)
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
                                adjustBrightness(activity, -fraction)
                            } else {
                                adjustVolume(audioManager, -fraction)
                            }
                        }
                    }
                )
            }
    )
}

private fun adjustBrightness(activity: Activity?, delta: Float) {
    val window = activity?.window ?: return
    val current = window.attributes.screenBrightness.let { if (it < 0f) 0.5f else it }
    val updated = (current + delta).coerceIn(0.05f, 1f)
    val params = window.attributes
    params.screenBrightness = updated
    window.attributes = params
}

private fun adjustVolume(audioManager: AudioManager?, delta: Float) {
    val am = audioManager ?: return
    val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
    val target = (current + (delta * max)).toInt().coerceIn(0, max)
    if (target != current) {
        am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
    }
}
