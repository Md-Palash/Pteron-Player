package com.pteron.player.util

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Two small pieces of shared, synchronously-readable state used by [com.pteron.player.MainActivity]
 * from plain (non-suspend, non-Composable) Activity callbacks:
 *
 *  - [isEligibleForAutoPip] / [videoAspectRatio]: updated by the player screen whenever it is on
 *    screen and actively playing, and read from `onUserLeaveHint()` to decide whether to
 *    automatically enter Picture-in-Picture when the user backgrounds the app.
 *  - [backgroundPlaybackEnabled]: mirrors the "Background play" Settings toggle. `onUserLeaveHint()`
 *    skips auto-PiP entirely when this is on, since the point of background play is to keep just
 *    the audio going (like a music player) rather than a floating video window.
 *
 * A tiny shared singleton is simpler and lighter than wiring a dependency-injected controller
 * through the navigation graph for a handful of primitives.
 */
object PipController {
    val isEligibleForAutoPip = MutableStateFlow(false)
    val videoAspectRatio = MutableStateFlow(16f / 9f)
    val backgroundPlaybackEnabled = MutableStateFlow(false)
}
