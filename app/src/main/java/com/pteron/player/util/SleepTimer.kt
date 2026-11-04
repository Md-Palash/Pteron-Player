package com.pteron.player.util

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArraySet

/**
 * One sleep timer for the whole app (video and music). Players register a listener that pauses
 * them when the time is up. Nothing runs while no timer is set.
 */
object SleepTimer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    private val listeners = CopyOnWriteArraySet<() -> Unit>()

    private val _endsAt = MutableStateFlow<Long?>(null)

    /** [SystemClock.elapsedRealtime] at which playback stops, or null when no timer is set. */
    val endsAt: StateFlow<Long?> = _endsAt.asStateFlow()

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    fun start(minutes: Int) {
        job?.cancel()
        val durationMs = minutes * 60_000L
        _endsAt.value = SystemClock.elapsedRealtime() + durationMs
        job = scope.launch {
            delay(durationMs)
            _endsAt.value = null
            listeners.forEach { it() }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _endsAt.value = null
    }
}
