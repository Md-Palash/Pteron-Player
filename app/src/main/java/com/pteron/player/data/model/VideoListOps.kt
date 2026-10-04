package com.pteron.player.data.model

import com.pteron.player.data.prefs.AppearanceState
import com.pteron.player.data.prefs.VideoPlaybackState

/**
 * The list logic shared by the Videos tab and the folder screen. Both run it on a background
 * dispatcher (never on the main thread), because sorting and filtering a library of thousands of
 * videos is exactly the kind of work that causes dropped frames when it happens while scrolling.
 */

/** Overlays the saved resume position, watched flag, favorite flag and subtitle info on each video. */
fun List<VideoItem>.hydrate(states: Map<Long, VideoPlaybackState>): List<VideoItem> {
    if (states.isEmpty()) return this
    return map { video ->
        val state = states[video.id]
        if (state == null) video else video.copy(
            lastPositionMs = state.lastPositionMs,
            isWatched = state.isWatched,
            isFavorite = state.isFavorite,
            hasEmbeddedSubtitleTrack = state.hasSubtitleTrack,
            lastPlayedAtMillis = state.lastPlayedAtMillis
        )
    }
}

/** Applies the search text and filter chip, then the chosen sort order and direction. */
fun List<VideoItem>.filteredAndSorted(
    query: String,
    filter: VideoFilter,
    appearance: AppearanceState
): List<VideoItem> {
    var list = this
    if (query.isNotBlank()) {
        list = list.filter { it.displayName.contains(query, ignoreCase = true) }
    }
    list = when (filter) {
        VideoFilter.ALL -> list
        VideoFilter.UNWATCHED -> list.filter { !it.isWatched }
        VideoFilter.FOUR_K -> list.filter { it.isFourK }
        VideoFilter.SUBTITLED -> list.filter { it.hasEmbeddedSubtitleTrack == true }
        VideoFilter.FAVORITES -> list.filter { it.isFavorite }
    }
    val sorted = when (appearance.sortOption) {
        SortOption.NAME -> list.sortedBy { it.displayName.lowercase() }
        SortOption.DATE_ADDED -> list.sortedBy { it.dateAddedSeconds }
        SortOption.SIZE -> list.sortedBy { it.sizeBytes }
        SortOption.DURATION -> list.sortedBy { it.durationMs }
        SortOption.RECENTLY_PLAYED -> list.sortedBy { it.lastPlayedAtMillis }
    }
    return if (appearance.sortDirection == SortDirection.DESCENDING) sorted.asReversed() else sorted
}
