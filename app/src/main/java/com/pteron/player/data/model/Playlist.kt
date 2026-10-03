package com.pteron.player.data.model

/**
 * A user-made playlist: an ordered list of MediaStore video ids. Only ids are stored -- titles,
 * thumbnails and durations are always read live from MediaStore, so a renamed or deleted file
 * never leaves stale data behind.
 */
data class Playlist(
    val id: Long,
    val name: String,
    val videoIds: List<Long>,
    val createdAtMillis: Long
)
