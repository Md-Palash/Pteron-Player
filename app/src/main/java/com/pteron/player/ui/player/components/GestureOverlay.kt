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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class SeekFlashSide {
    NONE,
    LEFT,
    RIGHT
}

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

    val audioManager = remember {
        context.getSystemService(AudioManager::class.java)
    }

    val scope = rememberCoroutineScope()

    // Use explicit .value / .floatValue instead of Compose "by" delegates.
    val scrubTargetMs = remember {
        mutableFloatStateOf(0f)
    }

    val isScrubbing = remember {
        mutableStateOf(false)
    }

    val dragStartedOnLeftHalf = remember {
        mutableStateOf(true)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(locked) {
                if (locked) {
                    return@pointerInput
                }

                detectTapGestures(
                    onTap = {
                        onToggleControls()
                    },
                    onDoubleTap = { offset ->
                        val third = size.width / 3f

                        when {
                            offset.x < third -> {
                                onSeekBy(-10_000L)
                                onFlash(SeekFlashSide.LEFT)
                            }

                            offset.x > third * 2f -> {
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
            .pointerInput(
                locked,
                sensitivity,
                durationMs,
                currentPositionMs
            ) {
                if (locked) {
                    return@pointerInput
                }

                detectDragGestures(
                    onDragStart = { offset ->
                        scrubTargetMs.floatValue =
                            currentPositionMs.toFloat()

                        dragStartedOnLeftHalf.value =
                            offset.x < size.width / 2f

                        isScrubbing.value = false
                    },

                    onDragEnd = {
                        if (isScrubbing.value) {
                            onScrubCommit(
                                scrubTargetMs.floatValue.toLong()
                            )

                            onScrubPreview(null)
                        }

                        isScrubbing.value = false
                    },

                    onDragCancel = {
                        if (isScrubbing.value) {
                            onScrubPreview(null)
                        }

                        isScrubbing.value = false
                    },

                    onDrag = { change, dragAmount ->
                        change.consume()

                        val isHorizontalGesture =
                            abs(dragAmount.x) >
                                    abs(dragAmount.y) * 1.3f

                        if (isHorizontalGesture && durationMs > 0) {

                            isScrubbing.value = true

                            val widthPx =
                                size.width.takeIf { it > 0 } ?: 1

                            // Full-width drag ≈ 2 minutes at 1.0x sensitivity.
                            val msPerPx =
                                (120_000f / widthPx) * sensitivity

                            val deltaMs =
                                dragAmount.x * msPerPx

                            scrubTargetMs.floatValue =
                                (
                                    scrubTargetMs.floatValue + deltaMs
                                ).coerceIn(
                                    0f,
                                    durationMs.toFloat()
                                )

                            onScrubPreview(
                                scrubTargetMs.floatValue.toLong()
                            )

                        } else if (!isHorizontalGesture) {

                            val heightPx =
                                size.height.takeIf { it > 0 } ?: 1

                            val fraction =
                                (dragAmount.y / heightPx) * sensitivity

                            if (dragStartedOnLeftHalf.value) {
                                adjustBrightness(
                                    activity,
                                    -fraction
                                )
                            } else {
                                adjustVolume(
                                    audioManager,
                                    -fraction
                                )
                            }
                        }
                    }
                )
            }
    )
}

private fun adjustBrightness(
    activity: Activity?,
    delta: Float
) {
    val window = activity?.window ?: return

    val current =
        window.attributes.screenBrightness.let {
            if (it < 0f) 0.5f else it
        }

    val updated =
        (current + delta).coerceIn(0.05f, 1f)

    val params = window.attributes
    params.screenBrightness = updated
    window.attributes = params
}

private fun adjustVolume(
    audioManager: AudioManager?,
    delta: Float
) {
    val am = audioManager ?: return

    val max =
        am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    val current =
        am.getStreamVolume(AudioManager.STREAM_MUSIC)

    val target =
        (current + delta * max)
            .toInt()
            .coerceIn(0, max)

    if (target != current) {
        am.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            target,
            0
        )
    }
}
