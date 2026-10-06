package com.pteron.player.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
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

/**
 * Cover art for a `content://media/.../audio/media/<id>` Uri. API 29+ asks MediaStore for its
 * cached artwork at the requested size; older versions (and files MediaStore has no art for) fall
 * back to the picture embedded in the file. Songs without any art fail the request, so the
 * placeholder glyph behind the image stays visible.
 */
class AudioThumbnailFetcher(
    private val context: Context,
    private val uri: Uri,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult = withContext(Dispatchers.IO) {
        val width = (options.size.width as? Dimension.Pixels)?.px ?: 400
        val height = (options.size.height as? Dimension.Pixels)?.px ?: 400
        val bitmap = platformThumbnail(width, height)
            ?: embeddedPicture(width.coerceAtLeast(height))
            ?: throw IllegalStateException("No artwork for $uri")
        DrawableResult(
            drawable = BitmapDrawable(context.resources, bitmap),
            isSampled = true,
            dataSource = DataSource.DISK
        )
    }

    private fun platformThumbnail(width: Int, height: Int): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return runCatching { context.contentResolver.loadThumbnail(uri, Size(width, height), null) }.getOrNull()
    }

    private fun embeddedPicture(target: Int): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val bytes = retriever.embeddedPicture ?: return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= target && bounds.outHeight / (sample * 2) >= target) sample *= 2
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
        } catch (t: Throwable) {
            null
        } finally {
            retriever.release()
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            // Decided from the Uri alone (no ContentResolver round trip): content://media/<volume>/audio/media/<id>.
            if (data.scheme != "content" || data.authority != MediaStore.AUTHORITY) return null
            return if (data.pathSegments.contains("audio")) AudioThumbnailFetcher(context, data, options) else null
        }
    }
}
