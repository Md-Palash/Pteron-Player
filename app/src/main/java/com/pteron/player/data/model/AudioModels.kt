package com.pteron.player.data.model

/** One song, backed by a real MediaStore audio row. [isFavorite] is overlaid from saved state. */
data class AudioItem(
    val id: Long,
    val contentUri: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAddedSeconds: Long,
    val isFavorite: Boolean = false
) {
    /** The system's album-art Uri; used as the artwork of the media notification / lock screen. */
    val albumArtUri: String get() = "content://media/external/audio/albumart/$albumId"
}

/** An artist and the songs that belong to it; [coverContentUri] is the first song, used for its art. */
data class AudioArtist(
    val name: String,
    val songCount: Int,
    val coverContentUri: String?
)
