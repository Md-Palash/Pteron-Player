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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
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

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _allVideos = MutableStateFlow<List<VideoItem>>(emptyList())
    private val _allFolders = MutableStateFlow<List<VideoFolder>>(emptyList())

    init {
        viewModelScope.launch {
            appearancePrefsRepository.state.collectLatest { appearance ->
                _uiState.value = _uiState.value.copy(appearance = appearance)
            }
        }
        viewModelScope.launch {
            mediaStoreRepository.observeMediaChanges().collectLatest {
                if (_uiState.value.permission == PermissionUiState.GRANTED) {
                    refresh()
                }
            }
        }
        viewModelScope.launch {
            combine(_allVideos, _allFolders, playbackStateRepository.allStates) { videos, folders, states ->
                Triple(videos, folders, states)
            }.collectLatest { (videos, folders, states) ->
                _uiState.value = _uiState.value.copy(
                    allFolders = folders,
                    continueWatching = buildContinueWatching(videos, states)
                )
            }
        }
    }

    fun onPermissionResult(granted: Boolean, canAskAgain: Boolean) {
        val newState = when {
            granted -> PermissionUiState.GRANTED
            !canAskAgain -> PermissionUiState.PERMANENTLY_DENIED
            else -> PermissionUiState.DENIED
        }
        _uiState.value = _uiState.value.copy(permission = newState)
        if (granted) refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                _allFolders.value = mediaStoreRepository.loadFolders()
                _allVideos.value = mediaStoreRepository.loadAllVideos()
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (t: Throwable) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = t.message ?: "Couldn't read your video library."
                )
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

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
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
