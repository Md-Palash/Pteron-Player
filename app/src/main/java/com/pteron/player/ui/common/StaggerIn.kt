package com.pteron.player.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Open while a screen is just appearing and closed shortly after, so only the first cards play
 * the entrance animation and cards scrolled into view later simply appear.
 */
class StaggerGate {
    @Volatile
    var closed: Boolean = false
}

@Composable
fun rememberStaggerGate(): StaggerGate {
    val gate = remember { StaggerGate() }
    LaunchedEffect(Unit) {
        delay(900L)
        gate.closed = true
    }
    return gate
}

/**
 * The same entrance the Settings cards have: each card fades and lifts in, one after another.
 * Only the draw phase animates, so it never recomposes the list.
 */
@Composable
fun Modifier.staggerIn(index: Int, gate: StaggerGate): Modifier {
    val animate = remember { !gate.closed && index < 14 }
    val progress = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(animate) {
        if (animate) {
            progress.animateTo(
                1f,
                tween(durationMillis = 300, delayMillis = 32 * index.coerceAtMost(10), easing = FastOutSlowInEasing)
            )
        }
    }
    return this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 18.dp.toPx()
    }
}
