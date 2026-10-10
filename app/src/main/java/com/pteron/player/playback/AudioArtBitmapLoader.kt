package com.pteron.player.playback

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceBitmapLoader
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * Loads the cover art shown in the media notification and on the lock screen.
 *
 * Media3's own loader cannot open a `content://media/.../audio/media/<id>` Uri, and the old
 * `albumart` Uri no longer works on Android 10+, so the notification used to have no picture.
 * Audio Uris are answered here (MediaStore thumbnail, or the picture embedded in the file); anything
 * else goes to the standard loader.
 */
@UnstableApi
class AudioArtBitmapLoader(private val context: Context) : BitmapLoader {

    private val standard = DataSourceBitmapLoader(context)
    private val executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())

    override fun supportsMimeType(mimeType: String): Boolean = standard.supportsMimeType(mimeType)

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> = standard.decodeBitmap(data)

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        val isAudio = uri.scheme == "content" &&
            uri.authority == MediaStore.AUTHORITY &&
            uri.pathSegments.contains("audio")
        if (!isAudio) return standard.loadBitmap(uri)
        return executor.submit(Callable<Bitmap> {
            loadAudioArt(uri) ?: throw IllegalStateException("No artwork for $uri")
        })
    }

    private fun loadAudioArt(uri: Uri): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching { context.contentResolver.loadThumbnail(uri, Size(640, 640), null) }
                .getOrNull()?.let { return it }
        }
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.embeddedPicture?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        } catch (t: Throwable) {
            null
        } finally {
            retriever.release()
        }
    }
}
