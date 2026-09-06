package com.pteron.player.data.mock

import com.pteron.player.data.model.FolderColor
import com.pteron.player.data.model.VideoFolder

/**
 * TEMPORARY Stage 1 data source.
 *
 * This exists purely so the UI has something believable to render
 * while the visual foundation is being built. Names and counts are
 * intentionally generic placeholders so nobody mistakes this for real
 * user data later.
 *
 * Stage 2+ should replace this with a real repository (e.g. backed by
 * MediaStore) behind the same [VideoFolder] shape, so the UI layer
 * does not need to change when the swap happens.
 */
object MockFolderRepository {

    fun getFolders(): List<VideoFolder> = listOf(
        VideoFolder(id = "1", name = "Sample Folder A", videoCount = 12, color = FolderColor.SAGE),
        VideoFolder(id = "2", name = "Sample Folder B", videoCount = 4, color = FolderColor.CLAY),
        VideoFolder(id = "3", name = "Sample Folder C", videoCount = 27, color = FolderColor.DUSK_BLUE),
        VideoFolder(id = "4", name = "Sample Folder D", videoCount = 1, color = FolderColor.MUTED_GOLD),
        VideoFolder(id = "5", name = "Sample Folder E", videoCount = 9, color = FolderColor.TEAL_MIST),
    )

    /** Used to exercise the empty-state design during development. */
    fun getEmptyFolders(): List<VideoFolder> = emptyList()
}
