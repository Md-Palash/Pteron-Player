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
    dominantBackground(bitmap)
}.getOrNull()

/**
 * The Now Playing background, from the colour that covers the largest AREA of the cover:
 *  - mostly black  -> (almost) black
 *  - any other colour -> a lighter shade of that same colour (lighter than the picture itself)
 * Pixels are sorted into black / light grey / grey / 12 hue families and the biggest group wins,
 * so a small bright spot can never outvote a big dark area.
 */
private fun dominantBackground(bitmap: Bitmap): Color? {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= 0 || h <= 0) return null
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
    val groups = 15
    val count = IntArray(groups)
    val sumR = DoubleArray(groups)
    val sumG = DoubleArray(groups)
    val sumB = DoubleArray(groups)
    val hsl = FloatArray(3)
    var total = 0
    for (p in pixels) {
        if ((p ushr 24) < 128) continue
        ColorUtils.colorToHSL(p, hsl)
        val group = when {
            hsl[2] < 0.14f -> 0
            hsl[1] < 0.12f -> if (hsl[2] > 0.8f) 1 else 2
            else -> 3 + ((hsl[0] / 30f).toInt() % 12)
        }
        count[group]++
        sumR[group] += ((p shr 16) and 0xFF).toDouble()
        sumG[group] += ((p shr 8) and 0xFF).toDouble()
        sumB[group] += (p and 0xFF).toDouble()
        total++
    }
    if (total == 0) return null
    var best = 0
    for (g in 1 until groups) if (count[g] > count[best]) best = g
    if (best == 0) return Color(0xFF0A0A0A)

    val n = count[best].toDouble()
    val avg = Color(
        (sumR[best] / n / 255.0).toFloat(),
        (sumG[best] / n / 255.0).toFloat(),
        (sumB[best] / n / 255.0).toFloat()
    )
    ColorUtils.colorToHSL(avg.toArgb(), hsl)
    if (best >= 3) hsl[1] = hsl[1].coerceAtMost(0.7f)
    hsl[2] = (hsl[2] + 0.18f).coerceIn(0.38f, 0.9f)
    return Color(ColorUtils.HSLToColor(hsl))
}
