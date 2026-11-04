package com.pteron.player.playback

import android.media.audiofx.Equalizer
import androidx.media3.common.C
import com.pteron.player.data.prefs.AudioMode

/**
 * Applies an [AudioMode] to one audio session through the platform equalizer. The effect only
 * exists while a non-flat mode is chosen, so "Normal" costs nothing (no native effect, no CPU).
 * Must be re-applied whenever ExoPlayer opens a new audio session.
 */
class AudioModeEffect {
    private var equalizer: Equalizer? = null

    fun apply(sessionId: Int, mode: AudioMode) {
        if (mode.isFlat || sessionId == C.AUDIO_SESSION_ID_UNSET) {
            release()
            return
        }
        if (equalizer == null) equalizer = runCatching { Equalizer(0, sessionId) }.getOrNull()
        val eq = equalizer ?: return
        runCatching {
            val range = eq.bandLevelRange
            val min = range[0].toInt()
            val max = range[1].toInt()
            for (band in 0 until eq.numberOfBands.toInt()) {
                val centerHz = eq.getCenterFreq(band.toShort()) / 1000f
                val level = (mode.gainDbAt(centerHz) * 100f).toInt().coerceIn(min, max)
                eq.setBandLevel(band.toShort(), level.toShort())
            }
            eq.enabled = true
        }
    }

    fun release() {
        equalizer?.release()
        equalizer = null
    }
}
