package com.pteron.player.ui.videos

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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * [visibleVideos] (search + filter + sort applied) is computed by the view model on a background
 * thread and carried in the state, instead of being recomputed on the main thread whenever the
 * screen reads it.
 */
data class VideosUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val videos: List<VideoItem> = emptyList(),
    val searchQuery: String = "",
    val activeFilter: VideoFilter = VideoFilter.ALL,
    val appearance: AppearanceState = AppearanceState(),
    val visibleVideos: List<VideoItem> = emptyList()
)

/** Backs the "Videos" bottom-nav tab: every video in the library, regardless of folder. */
class VideosViewModel(
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
    private var loadJob: Job? = null

    /**
     * Built only while the screen is on screen (plus 5 s of grace for rotation). While another
     * screen -- the player, above all -- is on top, nothing here runs: the resume position is saved
     * every few seconds during playback, and this used to re-process the whole library each time.
     */
    val uiState: StateFlow<VideosUiState> = combine(
        rawVideos, playbackStateRepository.allStates, appearancePrefsRepository.state, controls
    ) { videos, states, appearance, c ->
        val hydrated = videos.hydrate(states)
        VideosUiState(
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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VideosUiState())

    @OptIn(FlowPreview::class)
    private fun observeMediaChanges() {
        viewModelScope.launch {
            // A media scan can fire dozens of notifications in a burst; reload once it settles.
            mediaStoreRepository.observeMediaChanges().debounce(500).collect { refresh() }
        }
    }

    init {
        refresh()
        observeMediaChanges()
    }

    fun refresh() {
        // A newer refresh replaces one still running, instead of several full scans overlapping.
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            controls.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                rawVideos.value = mediaStoreRepository.loadAllVideos()
                controls.update { it.copy(isLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                controls.update { it.copy(isLoading = false, errorMessage = t.message ?: "Couldn't read your videos.") }
            }
        }
    }

    fun onSearchQueryChange(query: String) = controls.update { it.copy(searchQuery = query) }

    fun onFilterSelected(filter: VideoFilter) = controls.update { it.copy(activeFilter = filter) }

    fun onViewModeSelected(mode: ViewMode) {
        viewModelScope.launch { appearancePrefsRepository.setViewMode(mode) }
    }

    fun onSortSelected(option: SortOption, direction: SortDirection) {
        viewModelScope.launch {
            appearancePrefsRepository.setSortOption(option)
            appearancePrefsRepository.setSortDirection(direction)
        }
    }

    fun toggleFavorite(video: VideoItem) {
        viewModelScope.launch { playbackStateRepository.setFavorite(video.id, !video.isFavorite) }
    }

    fun toggleWatched(video: VideoItem) {
        viewModelScope.launch { playbackStateRepository.setWatched(video.id, !video.isWatched) }
    }

    fun clearProgress(video: VideoItem) {
        viewModelScope.launch { playbackStateRepository.clearPosition(video.id) }
    }

    fun toggleDarkMode() {
        viewModelScope.launch { appearancePrefsRepository.toggleDarkMode() }
    }

    class Factory(
        private val mediaStoreRepository: MediaStoreRepository,
        private val playbackStateRepository: PlaybackStateRepository,
        private val appearancePrefsRepository: AppearancePrefsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            VideosViewModel(mediaStoreRepository, playbackStateRepository, appearancePrefsRepository) as T
    }
}
