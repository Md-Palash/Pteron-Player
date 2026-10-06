package com.pteron.player.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.musicPrefsDataStore by preferencesDataStore(name = "music_prefs")

data class MusicPrefsState(
    /** Skips silent stretches inside a track. */
    val skipSilence: Boolean = false,
    val loudnessBoostEnabled: Boolean = false,
    /** 0f..100f, mapped to LoudnessEnhancer gain (up to 20 dB). */
    val loudnessBoostLevel: Float = 0f,
    /** 0f..100f, mapped to BassBoost strength. */
    val bassBoostLevel: Float = 0f,
    /** 0f..100f, mapped to Virtualizer ("surround") strength. */
    val virtualizerLevel: Float = 0f,
    /** Songs shorter than this are left out of the library (0 = show everything). */
    val minTrackSeconds: Int = 30,
    val pauseOnHeadphonesUnplugged: Boolean = true,
    val keepScreenOnInNowPlaying: Boolean = false,
    /** Jump to the Now Playing screen as soon as a song is started from a list. */
    val openNowPlayingOnPlay: Boolean = false
)

/** Settings > Music Player. Each control reads and writes a real key that the audio player applies. */
class MusicPrefsRepository(private val context: Context) {

    private object Keys {
        val SKIP_SILENCE = booleanPreferencesKey("skip_silence")
        val LOUDNESS_ENABLED = booleanPreferencesKey("loudness_boost_enabled")
        val LOUDNESS_LEVEL = floatPreferencesKey("loudness_boost_level")
        val BASS_LEVEL = floatPreferencesKey("bass_boost_level")
        val VIRTUALIZER_LEVEL = floatPreferencesKey("virtualizer_level")
        val MIN_TRACK_SECONDS = intPreferencesKey("min_track_seconds")
        val PAUSE_ON_UNPLUG = booleanPreferencesKey("pause_on_headphones_unplugged")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on_now_playing")
        val OPEN_NOW_PLAYING = booleanPreferencesKey("open_now_playing_on_play")
    }

    val state: Flow<MusicPrefsState> = context.musicPrefsDataStore.data.map { p ->
        MusicPrefsState(
            skipSilence = p[Keys.SKIP_SILENCE] ?: false,
            loudnessBoostEnabled = p[Keys.LOUDNESS_ENABLED] ?: false,
            loudnessBoostLevel = p[Keys.LOUDNESS_LEVEL] ?: 0f,
            bassBoostLevel = p[Keys.BASS_LEVEL] ?: 0f,
            virtualizerLevel = p[Keys.VIRTUALIZER_LEVEL] ?: 0f,
            minTrackSeconds = p[Keys.MIN_TRACK_SECONDS] ?: 30,
            pauseOnHeadphonesUnplugged = p[Keys.PAUSE_ON_UNPLUG] ?: true,
            keepScreenOnInNowPlaying = p[Keys.KEEP_SCREEN_ON] ?: false,
            openNowPlayingOnPlay = p[Keys.OPEN_NOW_PLAYING] ?: false
        )
    }

    suspend fun setSkipSilence(value: Boolean) = edit { it[Keys.SKIP_SILENCE] = value }
    suspend fun setLoudnessBoostEnabled(value: Boolean) = edit { it[Keys.LOUDNESS_ENABLED] = value }
    suspend fun setLoudnessBoostLevel(value: Float) = edit { it[Keys.LOUDNESS_LEVEL] = value.coerceIn(0f, 100f) }
    suspend fun setBassBoostLevel(value: Float) = edit { it[Keys.BASS_LEVEL] = value.coerceIn(0f, 100f) }
    suspend fun setVirtualizerLevel(value: Float) = edit { it[Keys.VIRTUALIZER_LEVEL] = value.coerceIn(0f, 100f) }
    suspend fun setMinTrackSeconds(value: Int) = edit { it[Keys.MIN_TRACK_SECONDS] = value.coerceAtLeast(0) }
    suspend fun setPauseOnHeadphonesUnplugged(value: Boolean) = edit { it[Keys.PAUSE_ON_UNPLUG] = value }
    suspend fun setKeepScreenOnInNowPlaying(value: Boolean) = edit { it[Keys.KEEP_SCREEN_ON] = value }
    suspend fun setOpenNowPlayingOnPlay(value: Boolean) = edit { it[Keys.OPEN_NOW_PLAYING] = value }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        context.musicPrefsDataStore.edit(block)
    }
}
