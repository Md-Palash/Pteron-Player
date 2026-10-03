package com.pteron.player.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pteron.player.data.model.Playlist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.playlistDataStore by preferencesDataStore(name = "playlists")

/**
 * Persists playlists as one small JSON document in DataStore (org.json ships with Android, so no
 * extra dependency). Every change is a single read-modify-write inside `edit {}`, which DataStore
 * serializes, so two quick taps can never overwrite each other.
 */
class PlaylistRepository(private val context: Context) {

    companion object {
        const val MAX_NAME_LENGTH = 40
    }

    private val key = stringPreferencesKey("playlists_json")

    val playlists: Flow<List<Playlist>> = context.playlistDataStore.data
        .map { decode(it[key]) }
        .distinctUntilChanged()

    fun observe(id: Long): Flow<Playlist?> =
        playlists.map { list -> list.firstOrNull { it.id == id } }.distinctUntilChanged()

    suspend fun get(id: Long): Playlist? = playlists.first().firstOrNull { it.id == id }

    /** Creates a playlist (optionally with a first video) and returns its id. */
    suspend fun create(name: String, firstVideoId: Long? = null): Long {
        var newId = 0L
        update { list ->
            newId = System.currentTimeMillis()
            while (list.any { it.id == newId }) newId++
            list + Playlist(newId, cleanName(name), listOfNotNull(firstVideoId), System.currentTimeMillis())
        }
        return newId
    }

    suspend fun rename(id: Long, name: String) = update { list ->
        list.map { if (it.id == id) it.copy(name = cleanName(name)) else it }
    }

    suspend fun delete(id: Long) = update { list -> list.filterNot { it.id == id } }

    /** Returns true if the video was added, false if it was already in the playlist. */
    suspend fun addVideo(playlistId: Long, videoId: Long): Boolean {
        var added = false
        update { list ->
            list.map { p ->
                if (p.id == playlistId && videoId !in p.videoIds) {
                    added = true
                    p.copy(videoIds = p.videoIds + videoId)
                } else p
            }
        }
        return added
    }

    suspend fun removeVideo(playlistId: Long, videoId: Long) = update { list ->
        list.map { p -> if (p.id == playlistId) p.copy(videoIds = p.videoIds - videoId) else p }
    }

    /** Moves a video one step up (delta -1) or down (+1) inside its playlist. */
    suspend fun moveVideo(playlistId: Long, videoId: Long, delta: Int) = update { list ->
        list.map { p ->
            if (p.id != playlistId) return@map p
            val from = p.videoIds.indexOf(videoId)
            val to = from + delta
            if (from < 0 || to !in p.videoIds.indices) return@map p
            val ids = p.videoIds.toMutableList()
            ids[from] = ids[to].also { ids[to] = ids[from] }
            p.copy(videoIds = ids)
        }
    }

    private suspend fun update(transform: (List<Playlist>) -> List<Playlist>) {
        context.playlistDataStore.edit { prefs ->
            prefs[key] = encode(transform(decode(prefs[key])))
        }
    }

    private fun cleanName(name: String): String =
        name.trim().take(MAX_NAME_LENGTH).ifBlank { "Untitled playlist" }

    private fun encode(list: List<Playlist>): String {
        val array = JSONArray()
        list.forEach { p ->
            val ids = JSONArray()
            p.videoIds.forEach { ids.put(it) }
            array.put(
                JSONObject()
                    .put("id", p.id)
                    .put("name", p.name)
                    .put("created", p.createdAtMillis)
                    .put("videos", ids)
            )
        }
        return array.toString()
    }

    private fun decode(json: String?): List<Playlist> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                val ids = o.optJSONArray("videos")
                Playlist(
                    id = o.getLong("id"),
                    name = o.optString("name", "Playlist"),
                    videoIds = (0 until (ids?.length() ?: 0)).map { ids!!.getLong(it) },
                    createdAtMillis = o.optLong("created", 0L)
                )
            }
        }.getOrDefault(emptyList())
    }
}
