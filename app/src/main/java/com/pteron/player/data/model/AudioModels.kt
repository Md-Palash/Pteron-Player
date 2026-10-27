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
)

/** An artist and the songs that belong to it; [coverContentUri] is the first song, used for its art. */
data class AudioArtist(
    val name: String,
    val songCount: Int,
    val coverContentUri: String?
)
