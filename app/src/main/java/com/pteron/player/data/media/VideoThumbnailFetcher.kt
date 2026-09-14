package com.pteron.player.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.Dimension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Loads a lightweight thumbnail for a `content://` video Uri.
 *
 * On API 29+ this uses [android.content.ContentResolver.loadThumbnail], which
 * asks MediaStore for its cached thumbnail at the requested size -- cheap and
 * fast, with no full video decode. On older versions it falls back to
 * [MediaMetadataRetriever.getFrameAtTime], which does decode a single frame
 * but is still far cheaper than decoding the whole file.
 *
 * Coil provides the memory/disk caching, request de-duplication, and
 * cancellation on top of this -- so thumbnails are decoded once per video,
 * lazily, and only for on-screen items.
 */
class VideoThumbnailFetcher(
    private val context: Context,
    private val uri: Uri,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult = withContext(Dispatchers.IO) {
        val targetWidth = (options.size.width as? Dimension.Pixels)?.px ?: 320
        val targetHeight = (options.size.height as? Dimension.Pixels)?.px ?: 180

        val bitmap = loadPlatformThumbnail(targetWidth, targetHeight)
            ?: loadRetrieverFrame()
            ?: throw IllegalStateException("No thumbnail available for $uri")

        val bytes = ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            stream.toByteArray()
        }
        bitmap.recycle()

        DrawableResult(
            drawable = BitmapDrawable(
                context.resources,
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ),
            isSampled = true,
            dataSource = DataSource.DISK
        )
    }

    private fun loadPlatformThumbnail(width: Int, height: Int): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return runCatching {
            context.contentResolver.loadThumbnail(uri, Size(width, height), null)
        }.getOrNull()
    }

    private fun loadRetrieverFrame(): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.frameAtTime ?: retriever.getFrameAtTime(0)
        } catch (t: Throwable) {
            null
        } finally {
            retriever.release()
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            val authority = data.authority ?: return null
            val isVideoContent = authority.contains("media") &&
                context.contentResolver.getType(data)?.startsWith("video/") == true
            return if (isVideoContent) VideoThumbnailFetcher(context, data, options) else null
        }
    }
}
