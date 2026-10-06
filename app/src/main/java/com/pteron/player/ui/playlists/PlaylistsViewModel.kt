package com.pteron.player.ui.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pteron.player.data.media.MediaStoreRepository
import com.pteron.player.data.model.Playlist
import com.pteron.player.data.model.VideoItem
import com.pteron.player.data.model.hydrate
import com.pteron.player.data.prefs.PlaybackStateRepository
import com.pteron.player.data.prefs.PlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Backs the Playlists tab. [playlists] is null only until the first read from disk finishes. */
class PlaylistsViewModel(private val repository: PlaylistRepository) : ViewModel() {

    val playlists: StateFlow<List<Playlist>?> =
        repository.playlists.stateIn<List<Playlist>?>(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun create(name: String) {
        viewModelScope.launch { repository.create(name) }
    }

    fun rename(id: Long, name: String) {
        viewModelScope.launch { repository.rename(id, name) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    class Factory(private val repository: PlaylistRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = PlaylistsViewModel(repository) as T
    }
}

data class PlaylistDetailUiState(
    val isLoading: Boolean = true,
    /** True once the playlist no longer exists (deleted): the screen closes itself. */
    val notFound: Boolean = false,
    val name: String = "",
    val videos: List<VideoItem> = emptyList()
)

/** Backs one playlist's page: its name and its videos, in playlist order, read live from MediaStore. */
class PlaylistDetailViewModel(
    private val playlistId: Long,
    private val repository: PlaylistRepository,
    private val mediaStoreRepository: MediaStoreRepository,
    private val playbackStateRepository: PlaybackStateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistDetailUiState())

    /** The playlist's videos with resume / watched / favorite state laid over them, like every other video list. */
    val uiState: StateFlow<PlaylistDetailUiState> = combine(_uiState, playbackStateRepository.allStates) { ui, states ->
        ui.copy(videos = ui.videos.hydrate(states))
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaylistDetailUiState())

    init {
        viewModelScope.launch {
            repository.observe(playlistId).collectLatest { playlist ->
                if (playlist == null) {
                    _uiState.value = _uiState.value.copy(isLoading = false, notFound = true)
                } else {
                    val videos = runCatching { mediaStoreRepository.loadVideosByIds(playlist.videoIds) }
                        .getOrDefault(emptyList())
                    _uiState.value = PlaylistDetailUiState(isLoading = false, name = playlist.name, videos = videos)
                }
            }
        }
    }

    fun rename(name: String) {
        viewModelScope.launch { repository.rename(playlistId, name) }
    }

    fun delete() {
        viewModelScope.launch { repository.delete(playlistId) }
    }

    fun remove(video: VideoItem) {
        viewModelScope.launch { repository.removeVideo(playlistId, video.id) }
    }

    fun move(video: VideoItem, delta: Int) {
        viewModelScope.launch { repository.moveVideo(playlistId, video.id, delta) }
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

    class Factory(
        private val playlistId: Long,
        private val repository: PlaylistRepository,
        private val mediaStoreRepository: MediaStoreRepository,
        private val playbackStateRepository: PlaybackStateRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PlaylistDetailViewModel(playlistId, repository, mediaStoreRepository, playbackStateRepository) as T
    }
}
