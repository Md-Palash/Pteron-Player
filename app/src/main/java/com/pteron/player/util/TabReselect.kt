package com.pteron.player.util

import com.pteron.player.navigation.BottomNavDestination
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * The bottom bar now lives once, above the navigation host. When the person taps the tab they are
 * already on, the bar can't reach into that screen -- it posts the tab here instead, and a screen
 * that has something to reset (Settings steps back out to its header list) listens for it.
 */
object TabReselect {
    val events = MutableSharedFlow<BottomNavDestination>(extraBufferCapacity = 1)
}
