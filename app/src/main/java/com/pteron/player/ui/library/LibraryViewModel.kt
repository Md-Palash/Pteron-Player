package com.pteron.player.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pteron.player.data.media.MediaStoreRepository
import com.pteron.player.data.model.VideoFolder
import com.pteron.player.data.model.VideoItem
import com.pteron.player.data.prefs.AppearancePrefsRepository
import com.pteron.player.data.prefs.AppearanceState
import com.pteron.player.data.prefs.PlaybackStateRepository
import com.pteron.player.data.prefs.VideoPlaybackState
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

enum class PermissionUiState { UNKNOWN, GRANTED, DENIED, PERMANENTLY_DENIED }

data class LibraryUiState(
    val permission: PermissionUiState = PermissionUiState.UNKNOWN,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val allFolders: List<VideoFolder> = emptyList(),
    val continueWatching: List<VideoItem> = emptyList(),
    val searchQuery: String = "",
    val appearance: AppearanceState = AppearanceState()
) {
    val filteredFolders: List<VideoFolder>
        get() = if (searchQuery.isBlank()) {
            allFolders
        } else {
            allFolders.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
}

class LibraryViewModel(
    private val mediaStoreRepository: MediaStoreRepository,
    private val playbackStateRepository: PlaybackStateRepository,
    private val appearancePrefsRepository: AppearancePrefsRepository
) : ViewModel() {

    private data class Local(
        val permission: PermissionUiState = PermissionUiState.UNKNOWN,
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val searchQuery: String = ""
    )

    private val local = MutableStateFlow(Local())
    private val allVideos = MutableStateFlow<List<VideoItem>>(emptyList())
    private val allFolders = MutableStateFlow<List<VideoFolder>>(emptyList())
    private var loadJob: Job? = null

    /**
     * Built only while the Library is on screen. The "Continue Watching" list is derived from the
     * whole library and the saved positions, which are rewritten every few seconds during playback;
     * with the player open on top this used to be recomputed over and over for a screen nobody saw.
     */
    val uiState: StateFlow<LibraryUiState> = combine(
        local, allFolders, allVideos, playbackStateRepository.allStates, appearancePrefsRepository.state
    ) { l, folders, videos, states, appearance ->
        LibraryUiState(
            permission = l.permission,
            isLoading = l.isLoading,
            errorMessage = l.errorMessage,
            allFolders = folders,
            continueWatching = buildContinueWatching(videos, states),
            searchQuery = l.searchQuery,
            appearance = appearance
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    @OptIn(FlowPreview::class)
    private fun observeMediaChanges() {
        viewModelScope.launch {
            // A media scan can fire dozens of notifications in a burst; reload once it settles.
            mediaStoreRepository.observeMediaChanges().debounce(500).collect {
                if (local.value.permission == PermissionUiState.GRANTED) refresh()
            }
        }
    }

    init {
        observeMediaChanges()
    }

    fun onPermissionResult(granted: Boolean, canAskAgain: Boolean) {
        val newState = when {
            granted -> PermissionUiState.GRANTED
            !canAskAgain -> PermissionUiState.PERMANENTLY_DENIED
            else -> PermissionUiState.DENIED
        }
        val alreadyLoaded = local.value.permission == PermissionUiState.GRANTED && allFolders.value.isNotEmpty()
        local.update { it.copy(permission = newState) }
        // Coming back to the Library tab re-reports the permission; the media observer keeps the
        // data fresh, so don't rescan the whole device just because the tab was reopened.
        if (granted && !alreadyLoaded) refresh()
    }

    fun refresh() {
        // A newer refresh replaces one still running, instead of several full scans overlapping.
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            local.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // One read of MediaStore: the folders are derived from the same rows as the videos.
                val videos = mediaStoreRepository.loadAllVideos()
                allFolders.value = mediaStoreRepository.foldersFrom(videos)
                allVideos.value = videos
                local.update { it.copy(isLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                local.update { it.copy(isLoading = false, errorMessage = t.message ?: "Couldn't read your video library.") }
            }
        }
    }

    private fun buildContinueWatching(
        videos: List<VideoItem>,
        states: Map<Long, VideoPlaybackState>
    ): List<VideoItem> {
        return videos.mapNotNull { video ->
            val state = states[video.id] ?: return@mapNotNull null
            if (state.lastPositionMs <= 0L || state.isWatched) return@mapNotNull null
            video.copy(lastPositionMs = state.lastPositionMs, isWatched = state.isWatched, isFavorite = state.isFavorite)
        }.sortedByDescending { states[it.id]?.lastPlayedAtMillis ?: 0L }
            .take(10)
    }

    fun onSearchQueryChange(query: String) = local.update { it.copy(searchQuery = query) }

    fun toggleDarkMode() {
        viewModelScope.launch { appearancePrefsRepository.toggleDarkMode() }
    }

    class Factory(
        private val mediaStoreRepository: MediaStoreRepository,
        private val playbackStateRepository: PlaybackStateRepository,
        private val appearancePrefsRepository: AppearancePrefsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LibraryViewModel(mediaStoreRepository, playbackStateRepository, appearancePrefsRepository) as T
        }
    }
}
