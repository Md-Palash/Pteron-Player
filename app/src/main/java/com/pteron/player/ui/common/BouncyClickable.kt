package com.pteron.player.ui.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * A drop-in replacement for `Modifier.clickable(onClick = ...)` that adds a
 * subtle spring-based scale-down on press, matching the tactile, "premium"
 * feel of the design -- with zero extra dependencies, since it's built
 * entirely from Compose's own animation APIs.
 *
 * Taps give a light haptic tick and long presses a firmer one. The system's own "touch
 * vibration" setting still applies, so people who turned vibration off feel nothing.
 *
 * Used on folder tiles, video cards, and other tappable surfaces throughout
 * the app so taps feel responsive rather than static.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.bouncyClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
    haptic: Boolean = true,
    /** False for big controls whose icon changes on tap: the press settles without a wobble. */
    bouncy: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptics = LocalHapticFeedback.current
    val scale = androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = if (bouncy) Spring.DampingRatioMediumBouncy else Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bouncyClickableScale"
    )
    return this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .combinedClickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            onLongClick = onLongClick?.let { callback ->
                {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    callback()
                }
            },
            onClick = {
                if (haptic) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
        )
}
