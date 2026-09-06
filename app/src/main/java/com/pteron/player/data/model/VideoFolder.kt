package com.pteron.player.data.model

/**
 * Placeholder folder model for Stage 1.
 *
 * In a later stage this will be populated from real on-device media
 * (e.g. via MediaStore) instead of mock content. The shape is kept
 * deliberately small so swapping the data source later doesn't ripple
 * through the UI layer.
 */
data class VideoFolder(
    val id: String,
    val name: String,
    val videoCount: Int,
    val color: FolderColor
)
