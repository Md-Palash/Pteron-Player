package com.pteron.player.ui.player.components

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Small preview pictures for the seek bar. One retriever stays open for the current video while
 * the finger is dragging (opening one per frame would be far too slow) and is closed as soon as
 * the player screen goes away. Frames are decoded already scaled down to [MAX_SIDE_PX], from the
 * nearest keyframe, so a 4K video costs about the same as a small one.
 */
class SeekPreviewLoader(private val context: Context) {

    private val lock = Mutex()
    private var retriever: MediaMetadataRetriever? = null
    private var source: String? = null
    private var dstWidth = MAX_SIDE_PX
    private var dstHeight = MAX_SIDE_PX * 9 / 16

    suspend fun frameAt(uri: String, positionMs: Long): Bitmap? = withContext(Dispatchers.IO) {
        lock.withLock {
            runCatching {
                if (retriever == null || source != uri) open(uri)
                val r = retriever ?: return@runCatching null
                val timeUs = positionMs * 1000L
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    r.getScaledFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, dstWidth, dstHeight)
                } else {
                    r.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let {
                        val scaled = Bitmap.createScaledBitmap(it, dstWidth, dstHeight, true)
                        if (scaled !== it) it.recycle()
                        scaled
                    }
                }
            }.getOrNull()
        }
    }

    private fun open(uri: String) {
        retriever?.release()
        retriever = null
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(context, Uri.parse(uri))
        } catch (t: Throwable) {
            r.release()
            throw t
        }
        var w = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
        var h = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
        val rotation = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        if (rotation == 90 || rotation == 270) {
            val swap = w
            w = h
            h = swap
        }
        if (w > 0 && h > 0) {
            val scale = MAX_SIDE_PX.toFloat() / maxOf(w, h)
            dstWidth = (w * scale).toInt().coerceAtLeast(2)
            dstHeight = (h * scale).toInt().coerceAtLeast(2)
        }
        retriever = r
        source = uri
    }

    /** Closes the retriever (off the main thread, and only once a running decode has finished). */
    fun release() {
        CoroutineScope(Dispatchers.IO).launch {
            lock.withLock {
                retriever?.release()
                retriever = null
                source = null
            }
        }
    }

    private companion object {
        const val MAX_SIDE_PX = 320
    }
}
