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
import com.pteron.player.data.model.filteredAndSorted
import com.pteron.player.data.model.hydrate
import com.pteron.player.data.prefs.AppearancePrefsRepository
import com.pteron.player.data.prefs.AppearanceState
import com.pteron.player.data.prefs.PlaybackStateRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** [visibleVideos] is computed off the main thread by the view model (see [VideosViewModel]). */
data class FolderUiState(
    val folderName: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val videos: List<VideoItem> = emptyList(),
    val searchQuery: String = "",
    val activeFilter: VideoFilter = VideoFilter.ALL,
    val appearance: AppearanceState = AppearanceState(),
    val visibleVideos: List<VideoItem> = emptyList()
)

class FolderViewModel(
    private val bucketId: String,
    private val folderName: String,
    private val mediaStoreRepository: MediaStoreRepository,
    private val playbackStateRepository: PlaybackStateRepository,
    private val appearancePrefsRepository: AppearancePrefsRepository
) : ViewModel() {

    private data class Controls(
        val isLoading: Boolean = true,
        val errorMessage: String? = null,
        val searchQuery: String = "",
        val activeFilter: VideoFilter = VideoFilter.ALL
    )

    private val controls = MutableStateFlow(Controls())
    private val rawVideos = MutableStateFlow<List<VideoItem>>(emptyList())

    /** Only active while the folder screen is on screen -- nothing runs while the player is on top. */
    val uiState: StateFlow<FolderUiState> = combine(
        rawVideos, playbackStateRepository.allStates, appearancePrefsRepository.state, controls
    ) { videos, states, appearance, c ->
        val hydrated = videos.hydrate(states)
        FolderUiState(
            folderName = folderName,
            isLoading = c.isLoading,
            errorMessage = c.errorMessage,
            videos = hydrated,
            searchQuery = c.searchQuery,
            activeFilter = c.activeFilter,
            appearance = appearance,
            visibleVideos = hydrated.filteredAndSorted(c.searchQuery, c.activeFilter, appearance)
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FolderUiState(folderName = folderName))

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            controls.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // Only this folder's rows, not the whole device library (see MediaStoreRepository).
                rawVideos.value = mediaStoreRepository.loadVideosInBucket(bucketId)
                controls.update { it.copy(isLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                controls.update { it.copy(isLoading = false, errorMessage = t.message ?: "Couldn't read this folder.") }
            }
        }
    }

    fun onSearchQueryChange(query: String) = controls.update { it.copy(searchQuery = query) }

    fun onFilterSelected(filter: VideoFilter) = controls.update { it.copy(activeFilter = filter) }

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

    /** Clears saved resume progress for a single video (removes it from Continue Watching). */
    fun clearProgress(video: VideoItem) {
        viewModelScope.launch { playbackStateRepository.clearPosition(video.id) }
    }

    /** Returns a shuffled play order starting from a random visible video, for the Shuffle Play FAB. */
    fun shufflePlayOrder(): List<VideoItem> = uiState.value.visibleVideos.shuffled()

    fun toggleDarkMode() {
        viewModelScope.launch { appearancePrefsRepository.toggleDarkMode() }
    }

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
