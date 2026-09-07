package com.pteron.player.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.playbackDataStore by preferencesDataStore(name = "playback_state")

data class VideoPlaybackState(
    val lastPositionMs: Long = 0L,
    val isWatched: Boolean = false,
    val isFavorite: Boolean = false,
    val lastPlayedAtMillis: Long = 0L,
    val hasSubtitleTrack: Boolean? = null
)

/**
 * Per-video state that is not part of MediaStore: resume position, watched
 * flag, and favorites. Keyed by the stable MediaStore row id.
 *
 * A video is marked watched automatically once playback crosses
 * [WATCHED_THRESHOLD_FRACTION] of its duration; it can also be toggled
 * manually from the UI.
 */
class PlaybackStateRepository(private val context: Context) {

    companion object {
        const val WATCHED_THRESHOLD_FRACTION = 0.9f
    }

    private fun positionKey(id: Long) = longPreferencesKey("pos_$id")
    private fun playedAtKey(id: Long) = longPreferencesKey("played_at_$id")
    private fun watchedSetKey() = stringSetPreferencesKey("watched_ids")
    private fun favoriteSetKey() = stringSetPreferencesKey("favorite_ids")
    private fun subtitledSetKey() = stringSetPreferencesKey("subtitled_ids")
    private fun noSubtitleSetKey() = stringSetPreferencesKey("no_subtitle_ids")

    fun observeState(id: Long): Flow<VideoPlaybackState> = context.playbackDataStore.data.map { prefs ->
        VideoPlaybackState(
            lastPositionMs = prefs[positionKey(id)] ?: 0L,
            isWatched = prefs[watchedSetKey()]?.contains(id.toString()) ?: false,
            isFavorite = prefs[favoriteSetKey()]?.contains(id.toString()) ?: false,
            lastPlayedAtMillis = prefs[playedAtKey(id)] ?: 0L,
            hasSubtitleTrack = subtitleFlag(prefs, id)
        )
    }

    /** Bulk read used when hydrating the library/folder lists. */
    val allStates: Flow<Map<Long, VideoPlaybackState>> = context.playbackDataStore.data.map { prefs ->
        val watched = prefs[watchedSetKey()].orEmpty()
        val favorites = prefs[favoriteSetKey()].orEmpty()
        val positionKeys = prefs.asMap().keys.filter { it.name.startsWith("pos_") }
        val ids = (watched + favorites + positionKeys.map { it.name.removePrefix("pos_") }).toSet()
        ids.mapNotNull { idString -> idString.toLongOrNull() }.associateWith { id ->
            VideoPlaybackState(
                lastPositionMs = prefs[longPreferencesKey("pos_$id")] ?: 0L,
                isWatched = watched.contains(id.toString()),
                isFavorite = favorites.contains(id.toString()),
                lastPlayedAtMillis = prefs[longPreferencesKey("played_at_$id")] ?: 0L,
                hasSubtitleTrack = subtitleFlag(prefs, id)
            )
        }
    }

    private fun subtitleFlag(prefs: androidx.datastore.preferences.core.Preferences, id: Long): Boolean? = when {
        prefs[subtitledSetKey()]?.contains(id.toString()) == true -> true
        prefs[noSubtitleSetKey()]?.contains(id.toString()) == true -> false
        else -> null
    }

    /** Recorded the first time a video is opened in the player, once real track info is known. */
    suspend fun recordSubtitleAvailability(id: Long, hasSubtitles: Boolean) {
        context.playbackDataStore.edit { prefs ->
            if (hasSubtitles) {
                prefs[subtitledSetKey()] = prefs[subtitledSetKey()].orEmpty() + id.toString()
                prefs[noSubtitleSetKey()] = prefs[noSubtitleSetKey()].orEmpty() - id.toString()
            } else {
                prefs[noSubtitleSetKey()] = prefs[noSubtitleSetKey()].orEmpty() + id.toString()
                prefs[subtitledSetKey()] = prefs[subtitledSetKey()].orEmpty() - id.toString()
            }
        }
    }

    suspend fun savePosition(id: Long, positionMs: Long, durationMs: Long) {
        context.playbackDataStore.edit { prefs ->
            prefs[positionKey(id)] = positionMs
            prefs[playedAtKey(id)] = System.currentTimeMillis()
            if (durationMs > 0 && positionMs >= durationMs * WATCHED_THRESHOLD_FRACTION) {
                val current = prefs[watchedSetKey()].orEmpty()
                prefs[watchedSetKey()] = current + id.toString()
            }
        }
    }

    suspend fun setWatched(id: Long, watched: Boolean) {
        context.playbackDataStore.edit { prefs ->
            val current = prefs[watchedSetKey()].orEmpty()
            prefs[watchedSetKey()] = if (watched) current + id.toString() else current - id.toString()
        }
    }

    suspend fun setFavorite(id: Long, favorite: Boolean) {
        context.playbackDataStore.edit { prefs ->
            val current = prefs[favoriteSetKey()].orEmpty()
            prefs[favoriteSetKey()] = if (favorite) current + id.toString() else current - id.toString()
        }
    }

    suspend fun clearPosition(id: Long) {
        context.playbackDataStore.edit { prefs -> prefs.remove(positionKey(id)) }
    }
}
