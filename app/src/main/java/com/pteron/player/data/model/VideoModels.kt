package com.pteron.player.data.model

/**
 * A folder on the device that contains one or more videos, as discovered
 * through MediaStore. Folder identity is the absolute bucket path, since
 * Android does not expose a stable folder id for MediaStore queries.
 */
data class VideoFolder(
    val bucketId: String,
    val name: String,
    val videoCount: Int,
    val totalSizeBytes: Long,
    val coverContentUri: String?,
    val mostRecentDateAddedSeconds: Long
)

/**
 * A single playable video, backed by a real MediaStore row. Every field here
 * is either read directly from MediaStore/MediaMetadataRetriever or derived
 * from persisted playback state -- nothing is fabricated.
 */
data class VideoItem(
    val id: Long,
    val contentUri: String,
    val displayName: String,
    val bucketId: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAddedSeconds: Long,
    val width: Int,
    val height: Int,
    val mimeType: String,
    // Populated lazily/best-effort; null means "unknown", never guessed.
    val hasEmbeddedSubtitleTrack: Boolean? = null,
    // Persisted, user/device-derived state (not part of the MediaStore row).
    val lastPositionMs: Long = 0L,
    val isWatched: Boolean = false,
    val isFavorite: Boolean = false
) {
    val isFourK: Boolean get() = width >= 3840 || height >= 3840
    val progressFraction: Float
        get() = if (durationMs <= 0L) 0f else (lastPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
}

enum class SortOption(val label: String) {
    NAME("Name"),
    DATE_ADDED("Date added"),
    SIZE("Size"),
    DURATION("Duration")
}

enum class SortDirection { ASCENDING, DESCENDING }

enum class VideoFilter(val label: String) {
    ALL("All"),
    UNWATCHED("Unwatched"),
    FOUR_K("4K Ultra HD"),
    SUBTITLED("Subtitled")
}

enum class ViewMode { GRID, LIST }
