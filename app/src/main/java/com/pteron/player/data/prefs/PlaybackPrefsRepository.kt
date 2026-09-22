package com.pteron.player.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pteron.player.data.model.AspectRatioMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.playbackPrefsDataStore by preferencesDataStore(name = "playback_prefs")

enum class OrientationLock(val displayName: String) {
    AUTO("Follow device rotation"),
    PORTRAIT("Always portrait"),
    LANDSCAPE("Always landscape")
}

data class PlaybackPrefsState(
    val resumePlaybackEnabled: Boolean = true,
    val autoPlayNext: Boolean = true,
    val doubleTapSeekSeconds: Int = 10,
    val audioBoostEnabled: Boolean = false,
    /** 0f..100f, mapped to LoudnessEnhancer millibel gain when applied. */
    val audioBoostLevel: Float = 0f,
    val subtitleTextSizeSp: Float = 18f,
    val keepScreenOnWhilePlaying: Boolean = true,
    val orientationLock: OrientationLock = OrientationLock.AUTO,
    val defaultAspectRatio: AspectRatioMode = AspectRatioMode.FILL,
    /** How long the player controls stay visible before auto-hiding, while playing and unlocked. */
    val controlAutoHideSeconds: Int = 4,
    /** Keep audio playing (video decode off) when the app is minimized, the screen locks, or a
     *  floating Picture-in-Picture window is closed -- like a music player, instead of pausing. */
    val backgroundPlaybackEnabled: Boolean = false
)

/**
 * Player-behavior settings -- the "MX Player style" preferences that change how
 * playback actually behaves, as opposed to [AppearancePrefsRepository] which
 * only changes how things look.
 */
class PlaybackPrefsRepository(private val context: Context) {

    private object Keys {
        val RESUME_PLAYBACK = booleanPreferencesKey("resume_playback_enabled")
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val DOUBLE_TAP_SEEK_SECONDS = intPreferencesKey("double_tap_seek_seconds")
        val AUDIO_BOOST_ENABLED = booleanPreferencesKey("audio_boost_enabled")
        val AUDIO_BOOST_LEVEL = floatPreferencesKey("audio_boost_level")
        val SUBTITLE_TEXT_SIZE = floatPreferencesKey("subtitle_text_size_sp")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on_while_playing")
        val ORIENTATION_LOCK = stringPreferencesKey("orientation_lock")
        val DEFAULT_ASPECT_RATIO = stringPreferencesKey("default_aspect_ratio")
        val CONTROL_AUTO_HIDE_SECONDS = intPreferencesKey("control_auto_hide_seconds")
        val BACKGROUND_PLAYBACK = booleanPreferencesKey("background_playback_enabled")
    }

    val state: Flow<PlaybackPrefsState> = context.playbackPrefsDataStore.data.map { prefs ->
        PlaybackPrefsState(
            resumePlaybackEnabled = prefs[Keys.RESUME_PLAYBACK] ?: true,
            autoPlayNext = prefs[Keys.AUTO_PLAY_NEXT] ?: true,
            doubleTapSeekSeconds = prefs[Keys.DOUBLE_TAP_SEEK_SECONDS] ?: 10,
            audioBoostEnabled = prefs[Keys.AUDIO_BOOST_ENABLED] ?: false,
            audioBoostLevel = prefs[Keys.AUDIO_BOOST_LEVEL] ?: 0f,
            subtitleTextSizeSp = prefs[Keys.SUBTITLE_TEXT_SIZE] ?: 18f,
            keepScreenOnWhilePlaying = prefs[Keys.KEEP_SCREEN_ON] ?: true,
            orientationLock = runCatching {
                OrientationLock.valueOf(prefs[Keys.ORIENTATION_LOCK] ?: OrientationLock.AUTO.name)
            }.getOrDefault(OrientationLock.AUTO),
            defaultAspectRatio = runCatching {
                AspectRatioMode.valueOf(prefs[Keys.DEFAULT_ASPECT_RATIO] ?: AspectRatioMode.FILL.name)
            }.getOrDefault(AspectRatioMode.FILL),
            controlAutoHideSeconds = prefs[Keys.CONTROL_AUTO_HIDE_SECONDS] ?: 4,
            backgroundPlaybackEnabled = prefs[Keys.BACKGROUND_PLAYBACK] ?: false
        )
    }

    suspend fun setResumePlaybackEnabled(value: Boolean) = edit { it[Keys.RESUME_PLAYBACK] = value }
    suspend fun setAutoPlayNext(value: Boolean) = edit { it[Keys.AUTO_PLAY_NEXT] = value }
    suspend fun setDoubleTapSeekSeconds(seconds: Int) = edit { it[Keys.DOUBLE_TAP_SEEK_SECONDS] = seconds }
    suspend fun setAudioBoostEnabled(value: Boolean) = edit { it[Keys.AUDIO_BOOST_ENABLED] = value }
    suspend fun setAudioBoostLevel(value: Float) = edit { it[Keys.AUDIO_BOOST_LEVEL] = value.coerceIn(0f, 100f) }
    suspend fun setSubtitleTextSize(sp: Float) = edit { it[Keys.SUBTITLE_TEXT_SIZE] = sp.coerceIn(12f, 28f) }
    suspend fun setKeepScreenOnWhilePlaying(value: Boolean) = edit { it[Keys.KEEP_SCREEN_ON] = value }
    suspend fun setOrientationLock(lock: OrientationLock) = edit { it[Keys.ORIENTATION_LOCK] = lock.name }
    suspend fun setDefaultAspectRatio(mode: AspectRatioMode) = edit { it[Keys.DEFAULT_ASPECT_RATIO] = mode.name }
    suspend fun setControlAutoHideSeconds(seconds: Int) = edit { it[Keys.CONTROL_AUTO_HIDE_SECONDS] = seconds.coerceIn(1, 15) }
    suspend fun setBackgroundPlaybackEnabled(value: Boolean) = edit { it[Keys.BACKGROUND_PLAYBACK] = value }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.playbackPrefsDataStore.edit(block)
    }
}
