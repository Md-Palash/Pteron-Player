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
    suspend fun loadAllVideos(): List<VideoItem> = queryVideos(selection = null, selectionArgs = null)

    /**
     * Loads only the videos in one folder. Used to open a folder or start playback, so neither
     * has to read and hold the entire device library (which can be thousands of rows) just to
     * pull out one bucket's worth of items -- lighter on RAM, CPU and battery alike.
     */
    suspend fun loadVideosInBucket(bucketId: String): List<VideoItem> = queryVideos(
        selection = "${MediaStore.Video.Media.BUCKET_ID} = ?",
        selectionArgs = arrayOf(bucketId)
    )

    /**
     * Loads specific videos by id and returns them in the order of [ids] (a playlist's order).
     * Ids that no longer exist on the device are simply left out.
     */
    suspend fun loadVideosByIds(ids: List<Long>): List<VideoItem> {
        if (ids.isEmpty()) return emptyList()
        val found = ids.distinct().chunked(500).flatMap { chunk ->
            queryVideos(
                selection = "${MediaStore.Video.Media._ID} IN (${chunk.joinToString(",") { "?" }})",
                selectionArgs = chunk.map { it.toString() }.toTypedArray()
            )
        }.associateBy { it.id }
        return ids.mapNotNull { found[it] }
    }

    private suspend fun queryVideos(selection: String?, selectionArgs: Array<String>?): List<VideoItem> =
        withContext(Dispatchers.IO) {
            val results = mutableListOf<VideoItem>()
            val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
                val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
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
                        bucketName = cursor.getString(bucketNameCol).orEmpty(),
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
    suspend fun loadFolders(): List<VideoFolder> = foldersFrom(loadAllVideos())

    /**
     * Builds the folder list from videos that are already loaded, so a screen that needs both the
     * videos and the folders reads MediaStore once instead of twice. The folder name comes from the
     * same rows (no extra query per folder, as there used to be).
     */
    suspend fun foldersFrom(videos: List<VideoItem>): List<VideoFolder> = withContext(Dispatchers.Default) {
        videos.groupBy { it.bucketId }.map { (bucketId, items) ->
            val name = items.first().bucketName.ifBlank { items.first().displayName.substringBeforeLast('.') }
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
