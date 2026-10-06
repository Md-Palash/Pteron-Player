package com.pteron.player.ui.audio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pteron.player.data.media.MediaStoreAudioRepository
import com.pteron.player.data.model.AudioArtist
import com.pteron.player.data.model.AudioItem
import com.pteron.player.data.model.Playlist
import com.pteron.player.data.prefs.AppearancePrefsRepository
import com.pteron.player.data.prefs.AudioStateRepository
import com.pteron.player.data.prefs.MusicPrefsRepository
import com.pteron.player.data.prefs.MusicPrefsState
import com.pteron.player.data.prefs.PlaylistRepository
import com.pteron.player.ui.library.PermissionUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AudioUiState(
    val permission: PermissionUiState = PermissionUiState.UNKNOWN,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val songs: List<AudioItem> = emptyList(),
    val recents: List<AudioItem> = emptyList(),
    val favorites: List<AudioItem> = emptyList(),
    val artists: List<AudioArtist> = emptyList(),
    /** Audio playlists; [Playlist.videoIds] holds song ids here. */
    val playlists: List<Playlist> = emptyList()
)

/**
 * Backs every screen of the Audio section. It is scoped to the activity (created once in the nav
 * graph), so the library, favorites and playlists are shared by the Audio tab, the artist and
 * playlist pages and the Now Playing screen, and nothing is reloaded when moving between them.
 */
class AudioViewModel(
    private val audioRepository: MediaStoreAudioRepository,
    private val stateRepository: AudioStateRepository,
    private val playlistRepository: PlaylistRepository,
    private val musicPrefsRepository: MusicPrefsRepository,
    private val appearancePrefsRepository: AppearancePrefsRepository
) : ViewModel() {

    private data class Local(
        val permission: PermissionUiState = PermissionUiState.UNKNOWN,
        val isLoading: Boolean = false,
        val errorMessage: String? = null
    )

    private val local = MutableStateFlow(Local())
    private val rawSongs = MutableStateFlow<List<AudioItem>>(emptyList())
    private var loadJob: Job? = null

    val uiState: StateFlow<AudioUiState> = combine(
        local, rawSongs, stateRepository.state, playlistRepository.playlists
    ) { l, songs, st, playlists ->
        val hydrated = songs.map { it.copy(isFavorite = it.id in st.favoriteIds) }
        val byId = hydrated.associateBy { it.id }
        AudioUiState(
            permission = l.permission,
            isLoading = l.isLoading,
            errorMessage = l.errorMessage,
            songs = hydrated,
            recents = st.recentIds.mapNotNull { byId[it] }.take(20),
            favorites = hydrated.filter { it.isFavorite },
            artists = hydrated.groupBy { it.artist }
                .map { (name, items) -> AudioArtist(name, items.size, items.first().contentUri) }
                .sortedBy { it.name.lowercase() },
            playlists = playlists
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AudioUiState())

    val musicPrefs: StateFlow<MusicPrefsState> =
        musicPrefsRepository.state.stateIn(viewModelScope, SharingStarted.Eagerly, MusicPrefsState())

    @OptIn(FlowPreview::class)
    private fun observeChanges() {
        viewModelScope.launch {
            // A media scan can fire dozens of notifications in a burst; reload once it settles.
            audioRepository.observeMediaChanges().debounce(500).collect {
                if (local.value.permission == PermissionUiState.GRANTED) refresh()
            }
        }
        viewModelScope.launch {
            // Changing "ignore short tracks" in Settings re-reads the library.
            musicPrefsRepository.state.map { it.minTrackSeconds }.distinctUntilChanged().drop(1).collect {
                if (local.value.permission == PermissionUiState.GRANTED) refresh()
            }
        }
    }

    init {
        observeChanges()
    }

    fun onPermissionResult(granted: Boolean, canAskAgain: Boolean) {
        val newState = when {
            granted -> PermissionUiState.GRANTED
            !canAskAgain -> PermissionUiState.PERMANENTLY_DENIED
            else -> PermissionUiState.DENIED
        }
        val alreadyLoaded = local.value.permission == PermissionUiState.GRANTED && rawSongs.value.isNotEmpty()
        local.update { it.copy(permission = newState) }
        if (granted && !alreadyLoaded) refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            local.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val minMs = musicPrefsRepository.state.first().minTrackSeconds
                rawSongs.value = audioRepository.loadSongs(minMs * 1000L)
                local.update { it.copy(isLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                local.update { it.copy(isLoading = false, errorMessage = t.message ?: "Couldn't read your music.") }
            }
        }
    }

    fun toggleFavorite(song: AudioItem) {
        viewModelScope.launch { stateRepository.setFavorite(song.id, !song.isFavorite) }
    }

    fun createPlaylist(name: String, firstSongId: Long? = null) {
        viewModelScope.launch { playlistRepository.create(name, firstSongId) }
    }

    fun renamePlaylist(id: Long, name: String) {
        viewModelScope.launch { playlistRepository.rename(id, name) }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch { playlistRepository.delete(id) }
    }

    fun addToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch { playlistRepository.addVideo(playlistId, songId) }
    }

    fun removeFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch { playlistRepository.removeVideo(playlistId, songId) }
    }

    fun moveInPlaylist(playlistId: Long, songId: Long, delta: Int) {
        viewModelScope.launch { playlistRepository.moveVideo(playlistId, songId, delta) }
    }

    fun toggleDarkMode() {
        viewModelScope.launch { appearancePrefsRepository.toggleDarkMode() }
    }

    class Factory(
        private val audioRepository: MediaStoreAudioRepository,
        private val stateRepository: AudioStateRepository,
        private val playlistRepository: PlaylistRepository,
        private val musicPrefsRepository: MusicPrefsRepository,
        private val appearancePrefsRepository: AppearancePrefsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AudioViewModel(audioRepository, stateRepository, playlistRepository, musicPrefsRepository, appearancePrefsRepository) as T
    }
}
