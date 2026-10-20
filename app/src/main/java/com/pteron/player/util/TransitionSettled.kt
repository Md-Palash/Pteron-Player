package com.pteron.player.util

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * False while the screen is still sliding in, true from the moment its enter transition has
 * finished (and stays true after that). Heavy content -- big grids, thumbnail rows -- waits for
 * this, so the first frames of the animation only draw the light parts of the screen and the
 * motion never stalls while a big list is being composed. A screen shown without any animation
 * (the first launch) is settled from the very first frame.
 */
@Composable
fun AnimatedVisibilityScope.rememberSettled(): Boolean {
    val done = transition.currentState == EnterExitState.Visible &&
        transition.targetState == EnterExitState.Visible
    var settled by remember { mutableStateOf(done) }
    LaunchedEffect(done) { if (done) settled = true }
    return settled
}
