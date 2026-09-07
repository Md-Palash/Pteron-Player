package com.pteron.player.data.media

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.pteron.player.data.model.VideoFolder
import com.pteron.player.data.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

/**
 * Single source of truth for real, on-device video media. Every read goes
 * through [MediaStore.Video.Media.EXTERNAL_CONTENT_URI] -- there is no mock
 * or seeded data path.
 *
 * Queries run on [Dispatchers.IO] and are one-shot (triggered by the caller
 * or by [observeMediaChanges]); there is no continuously running scan.
 */
class MediaStoreRepository(private val context: Context) {

    private val projection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.BUCKET_ID,
        MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.SIZE,
        MediaStore.Video.Media.DATE_ADDED,
        MediaStore.Video.Media.WIDTH,
        MediaStore.Video.Media.HEIGHT,
        MediaStore.Video.Media.MIME_TYPE
    )

    /** Loads every video visible to the app, across all folders. */
    suspend fun loadAllVideos(): List<VideoItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<VideoItem>()
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                results += VideoItem(
                    id = id,
                    contentUri = uri.toString(),
                    displayName = cursor.getString(nameCol) ?: "Untitled",
                    bucketId = cursor.getString(bucketIdCol) ?: "unknown",
                    durationMs = cursor.getLong(durationCol),
                    sizeBytes = cursor.getLong(sizeCol),
                    dateAddedSeconds = cursor.getLong(dateCol),
                    width = cursor.getInt(widthCol),
                    height = cursor.getInt(heightCol),
                    mimeType = cursor.getString(mimeCol) ?: "video/*"
                )
            }
        }
        results
    }

    /** Groups [loadAllVideos] results into folders, mirroring the device's real bucket structure. */
    suspend fun loadFolders(): List<VideoFolder> = withContext(Dispatchers.IO) {
        val videos = loadAllVideos()
        videos.groupBy { it.bucketId }.map { (bucketId, items) ->
            val name = bucketDisplayName(bucketId) ?: items.first().displayName.substringBeforeLast('.')
            VideoFolder(
                bucketId = bucketId,
                name = name,
                videoCount = items.size,
                totalSizeBytes = items.sumOf { it.sizeBytes },
                coverContentUri = items.maxByOrNull { it.dateAddedSeconds }?.contentUri,
                mostRecentDateAddedSeconds = items.maxOf { it.dateAddedSeconds }
            )
        }.sortedByDescending { it.mostRecentDateAddedSeconds }
    }

    private fun bucketDisplayName(bucketId: String): String? {
        var name: String? = null
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Video.Media.BUCKET_DISPLAY_NAME),
            "${MediaStore.Video.Media.BUCKET_ID} = ?",
            arrayOf(bucketId),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                name = cursor.getString(0)
            }
        }
        return name
    }

    /**
     * Emits whenever the video collection changes on disk (new file, deleted
     * file, etc.), so the UI can offer a refresh without polling.
     */
    fun observeMediaChanges(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        context.contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )
        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }
}
