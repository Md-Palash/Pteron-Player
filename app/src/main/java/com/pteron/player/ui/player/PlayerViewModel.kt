package com.pteron.player.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.pteron.player.data.media.MediaStoreRepository
import com.pteron.player.data.model.VideoItem
import com.pteron.player.data.prefs.AppearancePrefsRepository
import com.pteron.player.data.prefs.AppearanceState
import com.pteron.player.data.prefs.PlaybackStateRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class AspectRatioMode(val label: String) {
    FIT("Fit"),
    CROP("Crop"),
    STRETCH("16:9")
}

data class TrackOption(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val isSelected: Boolean
)

data class PlayerUiState(
    val currentVideo: VideoItem? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = true,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPercentage: Int = 0,
    val playbackSpeed: Float = 1.0f,
    val aspectRatioMode: AspectRatioMode = AspectRatioMode.STRETCH,
    val audioTracks: List<TrackOption> = emptyList(),
    val subtitleTracks: List<TrackOption> = emptyList(),
    val subtitlesEnabled: Boolean = true,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val error: String? = null,
    val appearance: AppearanceState = AppearanceState()
)

class PlayerViewModel(
    application: Application,
    private val mediaStoreRepository: MediaStoreRepository,
    private val playbackStateRepository: PlaybackStateRepository,
    private val appearancePrefsRepository: AppearancePrefsRepository
) : AndroidViewModel(application) {

    private val trackSelector = DefaultTrackSelector(application)
    val player: ExoPlayer = ExoPlayer.Builder(application)
        .setTrackSelector(trackSelector)
        .build()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var playlist: List<VideoItem> = emptyList()
    private var currentIndex: Int = 0
    private var positionSaveJob: Job? = null

    init {
        viewModelScope.launch {
            appearancePrefsRepository.state.collectLatest { appearance ->
                val startingUp = _uiState.value.currentVideo == null
                _uiState.value = _uiState.value.copy(
                    appearance = appearance,
                    playbackSpeed = if (startingUp) appearance.defaultPlaybackSpeed else _uiState.value.playbackSpeed
                )
            }
        }

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
                if (isPlaying) {
                    startPositionTicker()
                } else {
                    stopPositionTicker()
                    persistCurrentPosition()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _uiState.value = _uiState.value.copy(
                    isBuffering = playbackState == Player.STATE_BUFFERING,
                    durationMs = player.duration.coerceAtLeast(0L)
                )
                if (playbackState == Player.STATE_ENDED) {
                    onPlaybackEnded()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _uiState.value = _uiState.value.copy(
                    error = "Playback error: ${error.errorCodeName.lowercase().replace('_', ' ')}"
                )
            }

            override fun onTracksChanged(tracks: Tracks) {
                val subtitleOptions = buildTrackOptions(tracks, C.TRACK_TYPE_TEXT)
                _uiState.value = _uiState.value.copy(
                    audioTracks = buildTrackOptions(tracks, C.TRACK_TYPE_AUDIO),
                    subtitleTracks = subtitleOptions
                )
                _uiState.value.currentVideo?.let { video ->
                    viewModelScope.launch {
                        playbackStateRepository.recordSubtitleAvailability(video.id, subtitleOptions.isNotEmpty())
                    }
                }
            }
        })
    }

    /** Loads the folder this video belongs to (for previous/next/shuffle) and starts playback. */
    fun openVideo(videoId: Long, bucketId: String) {
        viewModelScope.launch {
            val allVideos = mediaStoreRepository.loadAllVideos()
            playlist = allVideos.filter { it.bucketId == bucketId }
            currentIndex = playlist.indexOfFirst { it.id == videoId }.coerceAtLeast(0)
            if (playlist.isEmpty()) {
                val single = allVideos.firstOrNull { it.id == videoId }
                if (single != null) {
                    playlist = listOf(single)
                    currentIndex = 0
                }
            }
            playCurrent()
        }
    }

    private fun playCurrent() {
        val video = playlist.getOrNull(currentIndex) ?: return
        viewModelScope.launch {
            val resumeMs = playbackStateRepository.observeState(video.id).first().lastPositionMs

            val mediaItem = MediaItem.fromUri(video.contentUri)
            player.setMediaItem(mediaItem, resumeMs)
            player.playbackParameters = player.playbackParameters.withSpeed(_uiState.value.playbackSpeed)
            player.prepare()
            player.playWhenReady = true

            _uiState.value = _uiState.value.copy(
                currentVideo = video,
                error = null,
                hasNext = currentIndex < playlist.lastIndex,
                hasPrevious = currentIndex > 0,
                durationMs = video.durationMs,
                currentPositionMs = resumeMs
            )
        }
    }

    private fun onPlaybackEnded() {
        val video = _uiState.value.currentVideo ?: return
        viewModelScope.launch {
            playbackStateRepository.setWatched(video.id, true)
            playbackStateRepository.clearPosition(video.id)
        }
        if (_uiState.value.hasNext) skipToNext()
    }

    fun playPause() {
        player.playWhenReady = !player.playWhenReady
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceIn(0, player.duration.coerceAtLeast(0)))
    }

    fun seekBy(deltaMs: Long) {
        val target = (player.currentPosition + deltaMs).coerceIn(0, player.duration.coerceAtLeast(0))
        player.seekTo(target)
    }

    fun skipToNext() {
        if (currentIndex < playlist.lastIndex) {
            persistCurrentPosition()
            currentIndex++
            playCurrent()
        }
    }

    fun skipToPrevious() {
        if (currentIndex > 0) {
            persistCurrentPosition()
            currentIndex--
            playCurrent()
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        player.playbackParameters = player.playbackParameters.withSpeed(speed)
        _uiState.value = _uiState.value.copy(playbackSpeed = speed)
    }

    fun cycleAspectRatio() {
        val next = when (_uiState.value.aspectRatioMode) {
            AspectRatioMode.STRETCH -> AspectRatioMode.FIT
            AspectRatioMode.FIT -> AspectRatioMode.CROP
            AspectRatioMode.CROP -> AspectRatioMode.STRETCH
        }
        _uiState.value = _uiState.value.copy(aspectRatioMode = next)
    }

    fun selectAudioTrack(option: TrackOption) {
        trackSelector.parameters = trackSelector.parameters.buildUpon()
            .setOverrideForType(
                androidx.media3.common.TrackSelectionOverride(
                    player.currentTracks.groups[option.groupIndex].mediaTrackGroup,
                    option.trackIndex
                )
            )
            .build()
    }

    fun selectSubtitleTrack(option: TrackOption?) {
        val builder = trackSelector.parameters.buildUpon()
        if (option == null) {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            _uiState.value = _uiState.value.copy(subtitlesEnabled = false)
        } else {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            builder.setOverrideForType(
                androidx.media3.common.TrackSelectionOverride(
                    player.currentTracks.groups[option.groupIndex].mediaTrackGroup,
                    option.trackIndex
                )
            )
            _uiState.value = _uiState.value.copy(subtitlesEnabled = true)
        }
        trackSelector.parameters = builder.build()
    }

    fun toggleFavoriteCurrent() {
        val video = _uiState.value.currentVideo ?: return
        viewModelScope.launch { playbackStateRepository.setFavorite(video.id, !video.isFavorite) }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun buildTrackOptions(tracks: Tracks, type: Int): List<TrackOption> {
        val options = mutableListOf<TrackOption>()
        tracks.groups.forEachIndexed { groupIndex, group ->
            if (group.type != type) return@forEachIndexed
            for (trackIndex in 0 until group.length) {
                if (!group.isTrackSupported(trackIndex)) continue
                val format = group.getTrackFormat(trackIndex)
                val label = format.label
                    ?: format.language?.uppercase()
                    ?: "Track ${options.size + 1}"
                options += TrackOption(
                    groupIndex = groupIndex,
                    trackIndex = trackIndex,
                    label = label,
                    isSelected = group.isTrackSelected(trackIndex)
                )
            }
        }
        return options
    }

    private fun startPositionTicker() {
        stopPositionTicker()
        positionSaveJob = viewModelScope.launch {
            while (true) {
                val video = _uiState.value.currentVideo
                _uiState.value = _uiState.value.copy(
                    currentPositionMs = player.currentPosition.coerceAtLeast(0),
                    bufferedPercentage = player.bufferedPercentage
                )
                if (video != null && player.currentPosition > 0) {
                    playbackStateRepository.savePosition(video.id, player.currentPosition, player.duration.coerceAtLeast(0))
                }
                delay(1000)
            }
        }
    }

    private fun stopPositionTicker() {
        positionSaveJob?.cancel()
        positionSaveJob = null
    }

    private fun persistCurrentPosition() {
        val video = _uiState.value.currentVideo ?: return
        viewModelScope.launch {
            playbackStateRepository.savePosition(video.id, player.currentPosition, player.duration.coerceAtLeast(0))
        }
    }

    override fun onCleared() {
        persistCurrentPosition()
        stopPositionTicker()
        player.release()
        super.onCleared()
    }

    class Factory(
        private val application: Application,
        private val mediaStoreRepository: MediaStoreRepository,
        private val playbackStateRepository: PlaybackStateRepository,
        private val appearancePrefsRepository: AppearancePrefsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return PlayerViewModel(application, mediaStoreRepository, playbackStateRepository, appearancePrefsRepository) as T
        }
    }
}
