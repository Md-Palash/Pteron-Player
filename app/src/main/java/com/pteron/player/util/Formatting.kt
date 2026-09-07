package com.pteron.player.util

import java.util.Locale
import kotlin.math.roundToInt

/** Formats milliseconds as `H:MM:SS` or `MM:SS`, matching the Stitch timecode style. */
fun formatTimecode(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

/** Formats a remaining-time timecode with a leading minus sign, e.g. `-1:03:57`. */
fun formatRemaining(remainingMs: Long): String {
    if (remainingMs <= 0) return "0:00"
    return "-" + formatTimecode(remainingMs)
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.lastIndex) {
        value /= 1024
        unitIndex++
    }
    val rounded = (value * 10).roundToInt() / 10.0
    return if (unitIndex == 0) "${bytes} B" else String.format(Locale.US, "%.1f %s", rounded, units[unitIndex])
}

fun formatPercent(fraction: Float): String = "${(fraction * 100).roundToInt()}%"
