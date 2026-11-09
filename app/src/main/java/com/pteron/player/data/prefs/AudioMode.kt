package com.pteron.player.data.prefs

import kotlin.math.ln

/**
 * Ready-made sound profiles ("audio modes"). Each one is a gain curve in dB at five anchor
 * frequencies (60, 230, 910, 3600 and 14000 Hz). The curve is interpolated onto whatever bands the
 * device's equalizer really has, so a mode sounds the same on a 5-band and on a 10-band device.
 */
enum class AudioMode(
    val displayName: String,
    private val gains: FloatArray,
    /** False for profiles that only make sense for video (they are not offered in the music player). */
    val musicMode: Boolean = true
) {
    NORMAL("Normal", floatArrayOf(0f, 0f, 0f, 0f, 0f)),
    BASS_BOOST("Bass boost", floatArrayOf(6f, 4f, 1f, 0f, 0f)),
    VOCAL("Vocal", floatArrayOf(-2f, -1f, 3f, 4f, 1f)),
    TREBLE("Treble", floatArrayOf(-1f, 0f, 0f, 3f, 5f)),
    ROCK("Rock", floatArrayOf(4f, 2f, -2f, 2f, 4f)),
    POP("Pop", floatArrayOf(-1f, 2f, 3f, 2f, 0f)),
    JAZZ("Jazz", floatArrayOf(3f, 2f, -1f, 2f, 3f)),
    CLASSICAL("Classical", floatArrayOf(3f, 2f, 0f, 1f, 3f)),
    DANCE("Dance", floatArrayOf(5f, 3f, 0f, 2f, 3f)),
    /** Dialogue boost for video: lifts the speech range (1-4 kHz), softens rumble. */
    CLEAR_VOICE("Clear voice", floatArrayOf(-5f, -3f, 2f, 5f, 2f), musicMode = false);

    val isFlat: Boolean get() = this == NORMAL

    /** Gain in dB this mode applies at [freqHz]. */
    fun gainDbAt(freqHz: Float): Float {
        val x = ln(freqHz.coerceAtLeast(1f))
        if (x <= ln(ANCHORS[0])) return gains[0]
        for (i in 1 until ANCHORS.size) {
            val hi = ln(ANCHORS[i])
            if (x <= hi) {
                val lo = ln(ANCHORS[i - 1])
                val t = (x - lo) / (hi - lo)
                return gains[i - 1] + (gains[i] - gains[i - 1]) * t
            }
        }
        return gains.last()
    }

    companion object {
        private val ANCHORS = floatArrayOf(60f, 230f, 910f, 3600f, 14000f)
        val DEFAULT = NORMAL
        val musicModes: List<AudioMode> get() = entries.filter { it.musicMode }

        fun fromName(name: String?): AudioMode = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
