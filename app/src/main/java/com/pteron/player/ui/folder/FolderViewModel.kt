package com.pteron.player.ui.folder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pteron.player.data.media.MediaStoreRepository
import com.pteron.player.data.model.SortDirection
import com.pteron.player.data.model.SortOption
import com.pteron.player.data.model.VideoFilter
import com.pteron.player.data.model.VideoItem
import com.pteron.player.data.model.ViewMode
import com.pteron.player.data.prefs.AppearancePrefsRepository
import com.pteron.player.data.prefs.AppearanceState
import com.pteron.player.data.prefs.PlaybackStateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class FolderUiState(
    val folderName: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val videos: List<VideoItem> = emptyList(),
    val searchQuery: String = "",
    val activeFilter: VideoFilter = VideoFilter.ALL,
    val appearance: AppearanceState = AppearanceState()
) {
    val visibleVideos: List<VideoItem>
        get() {
            var list = videos
            if (searchQuery.isNotBlank()) {
                list = list.filter { it.displayName.contains(searchQuery, ignoreCase = true) }
            }
            list = when (activeFilter) {
                VideoFilter.ALL -> list
                VideoFilter.UNWATCHED -> list.filter { !it.isWatched }
                VideoFilter.FOUR_K -> list.filter { it.isFourK }
                VideoFilter.SUBTITLED -> list.filter { it.hasEmbeddedSubtitleTrack == true }
            }
            val sorted = when (appearance.sortOption) {
                SortOption.NAME -> list.sortedBy { it.displayName.lowercase() }
                SortOption.DATE_ADDED -> list.sortedBy { it.dateAddedSeconds }
                SortOption.SIZE -> list.sortedBy { it.sizeBytes }
                SortOption.DURATION -> list.sortedBy { it.durationMs }
            }
            return if (appearance.sortDirection == SortDirection.DESCENDING) sorted.reversed() else sorted
        }
}

class FolderViewModel(
    private val bucketId: String,
    folderName: String,
    private val mediaStoreRepository: MediaStoreRepository,
    private val playbackStateRepository: PlaybackStateRepository,
    private val appearancePrefsRepository: AppearancePrefsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FolderUiState(folderName = folderName))
    val uiState: StateFlow<FolderUiState> = _uiState.asStateFlow()

    private val _rawVideos = MutableStateFlow<List<VideoItem>>(emptyList())

    init {
        load()
        viewModelScope.launch {
            combine(_rawVideos, playbackStateRepository.allStates, appearancePrefsRepository.state) { videos, states, appearance ->
                val hydrated = videos.map { video ->
                    val state = states[video.id]
                    if (state == null) video else video.copy(
                        lastPositionMs = state.lastPositionMs,
                        isWatched = state.isWatched,
                        isFavorite = state.isFavorite,
                        hasEmbeddedSubtitleTrack = state.hasSubtitleTrack
                    )
                }
                Pair(hydrated, appearance)
            }.collectLatest { (hydrated, appearance) ->
                _uiState.value = _uiState.value.copy(videos = hydrated, appearance = appearance)
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val allVideos = mediaStoreRepository.loadAllVideos()
                _rawVideos.value = allVideos.filter { it.bucketId == bucketId }
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (t: Throwable) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = t.message ?: "Couldn't read this folder."
                )
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun onFilterSelected(filter: VideoFilter) {
        _uiState.value = _uiState.value.copy(activeFilter = filter)
    }

    fun onSortSelected(option: SortOption, direction: SortDirection) {
        viewModelScope.launch {
            appearancePrefsRepository.setSortOption(option)
            appearancePrefsRepository.setSortDirection(direction)
        }
    }

    fun onViewModeSelected(mode: ViewMode) {
        viewModelScope.launch { appearancePrefsRepository.setViewMode(mode) }
    }

    fun toggleFavorite(video: VideoItem) {
        viewModelScope.launch { playbackStateRepository.setFavorite(video.id, !video.isFavorite) }
    }

    fun toggleWatched(video: VideoItem) {
        viewModelScope.launch { playbackStateRepository.setWatched(video.id, !video.isWatched) }
    }

    /** Returns a shuffled play order starting from a random visible video, for the Shuffle Play FAB. */
    fun shufflePlayOrder(): List<VideoItem> = _uiState.value.visibleVideos.shuffled()

    class Factory(
        private val bucketId: String,
        private val folderName: String,
        private val mediaStoreRepository: MediaStoreRepository,
        private val playbackStateRepository: PlaybackStateRepository,
        private val appearancePrefsRepository: AppearancePrefsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FolderViewModel(
                bucketId, folderName, mediaStoreRepository, playbackStateRepository, appearancePrefsRepository
            ) as T
        }
    }
}
