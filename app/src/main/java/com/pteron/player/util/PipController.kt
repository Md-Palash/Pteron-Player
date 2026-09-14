package com.pteron.player.util

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * The player screen updates this whenever it is on screen and actively
 * playing, and [com.pteron.player.MainActivity] reads it from
 * `onUserLeaveHint()` to decide whether to automatically enter
 * Picture-in-Picture when the user backgrounds the app.
 *
 * A tiny shared singleton is simpler and lighter than wiring a
 * dependency-injected controller through the navigation graph for a single
 * boolean + aspect ratio pair.
 */
object PipController {
    val isEligibleForAutoPip = MutableStateFlow(false)
    val videoAspectRatio = MutableStateFlow(16f / 9f)
}
