package com.pteron.player.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.audioStateDataStore by preferencesDataStore(name = "audio_state")

/** [recentIds] is newest first. */
data class AudioStateData(
    val favoriteIds: Set<Long> = emptySet(),
    val recentIds: List<Long> = emptyList()
)

/** Per-song state that is not part of MediaStore: favorites and the recently-played list. */
class AudioStateRepository(private val context: Context) {

    companion object {
        const val MAX_RECENT = 50
    }

    private val favoritesKey = stringSetPreferencesKey("favorite_ids")
    private val recentKey = stringPreferencesKey("recent_ids")

    val state: Flow<AudioStateData> = context.audioStateDataStore.data.map { prefs ->
        AudioStateData(
            favoriteIds = prefs[favoritesKey].orEmpty().mapNotNull { it.toLongOrNull() }.toSet(),
            recentIds = prefs[recentKey].orEmpty().split(',').mapNotNull { it.toLongOrNull() }
        )
    }.distinctUntilChanged()

    suspend fun setFavorite(id: Long, favorite: Boolean) {
        context.audioStateDataStore.edit { prefs ->
            val current = prefs[favoritesKey].orEmpty()
            prefs[favoritesKey] = if (favorite) current + id.toString() else current - id.toString()
        }
    }

    suspend fun recordPlayed(id: Long) {
        context.audioStateDataStore.edit { prefs ->
            val current = prefs[recentKey].orEmpty().split(',').mapNotNull { it.toLongOrNull() }
            prefs[recentKey] = (listOf(id) + current.filter { it != id }).take(MAX_RECENT).joinToString(",")
        }
    }

    suspend fun clearRecent() {
        context.audioStateDataStore.edit { it.remove(recentKey) }
    }
}
