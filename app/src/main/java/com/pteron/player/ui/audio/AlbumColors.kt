package com.pteron.player.ui.audio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/** A dark, softly saturated version of [color]: the background of the Now Playing screen. */
fun deepTint(color: Color): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(color.toArgb(), hsl)
    hsl[1] = (hsl[1] * 1.25f).coerceIn(0f, 0.55f)
    hsl[2] = 0.17f
    return Color(ColorUtils.HSLToColor(hsl))
}

/**
 * The background color for a song, taken from its cover art. While a new cover is still being read
 * the previous color stays, so the screen never flashes; [fallback] is used when there is no art.
 */
@Composable
fun rememberArtTint(contentUri: String?, fallback: Color): Color {
    val context = LocalContext.current
    val tint = produceState(initialValue = fallback, contentUri, fallback) {
        value = contentUri?.let { uri ->
            withContext(Dispatchers.Default) { extractTint(context, uri) }
        } ?: fallback
    }
    return tint.value
}

private suspend fun extractTint(context: Context, contentUri: String): Color? = runCatching {
    // A tiny copy of the cover is enough to find its mood, and it is cached by Coil.
    val request = ImageRequest.Builder(context)
        .data(Uri.parse(contentUri))
        .size(64)
        .allowHardware(false)
        .build()
    val result = context.imageLoader.execute(request) as? SuccessResult ?: return@runCatching null
    val bitmap = (result.drawable as? BitmapDrawable)?.bitmap ?: return@runCatching null
    averageTint(bitmap)
}.getOrNull()

/** Average of the picture's colors, with colorful pixels counting far more than grey ones. */
private fun averageTint(bitmap: Bitmap): Color? {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= 0 || h <= 0) return null
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
    var r = 0.0
    var g = 0.0
    var b = 0.0
    var total = 0.0
    for (p in pixels) {
        val pr = (p shr 16) and 0xFF
        val pg = (p shr 8) and 0xFF
        val pb = p and 0xFF
        val chroma = (max(pr, max(pg, pb)) - min(pr, min(pg, pb))) / 255.0
        val weight = 0.05 + chroma * chroma
        r += pr * weight
        g += pg * weight
        b += pb * weight
        total += weight
    }
    if (total <= 0.0) return null
    return deepTint(Color((r / total / 255.0).toFloat(), (g / total / 255.0).toFloat(), (b / total / 255.0).toFloat()))
}
