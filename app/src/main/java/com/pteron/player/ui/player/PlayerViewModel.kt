package com.pteron.player.ui.player

import android.app.Application
import android.content.Intent
import android.media.audiofx.LoudnessEnhancer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import com.pteron.player.data.media.MediaStoreRepository
import com.pteron.player.data.model.AspectRatioMode
import com.pteron.player.data.model.VideoItem
import com.pteron.player.data.prefs.AppearancePrefsRepository
import com.pteron.player.data.prefs.AppearanceState
import com.pteron.player.data.prefs.PlaybackPrefsRepository
import com.pteron.player.data.prefs.PlaybackPrefsState
import com.pteron.player.data.prefs.PlaybackStateRepository
import com.pteron.player.playback.PlaybackSessionHolder
import com.pteron.player.playback.PlaybackSessionService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class RepeatMode(val label: String) {
    OFF("Off"),
    ONE("Repeat one"),
    ALL("Repeat all")
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
    val aspectRatioMode: AspectRatioMode = AspectRatioMode.FILL,
    val audioTracks: List<TrackOption> = emptyList(),
    val subtitleTracks: List<TrackOption> = emptyList(),
    val subtitlesEnabled: Boolean = true,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val isMuted: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleEnabled: Boolean = false,
    val error: String? = null,
    val appearance: AppearanceState = AppearanceState(),
    val playbackPrefs: PlaybackPrefsState = PlaybackPrefsState(),
    /** Best-effort: null until a decoder has actually initialized for the current video. */
    val isHardwareDecoder: Boolean? = null,
    /** Video frame rate reported by the selected track's format, if any. */
    val frameRate: Float? = null
)

class PlayerViewModel(
    private val application: Application,
    private val mediaStoreRepository: MediaStoreRepository,
    private val playbackStateRepository: PlaybackStateRepository,
    private val appearancePrefsRepository: AppearancePrefsRepository,
    private val playbackPrefsRepository: PlaybackPrefsRepository
) : AndroidViewModel(application) {

    private val trackSelector = DefaultTrackSelector(application)
    val player: ExoPlayer = ExoPlayer.Builder(application)
        .setTrackSelector(trackSelector)
        .build()

    // Exposes this player to the system (notification + lock screen controls,
    // headphone/Bluetooth media-button routing) via a MediaSessionService that
    // shares this in-process session rather than owning a separate player --
    // see PlaybackSessionHolder for why that's safe.
    private val mediaSession: MediaSession = MediaSession.Builder(application, player).build()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    /** mediaId (VideoItem.id.toString()) -> VideoItem, rebuilt each time a playlist is opened. */
    private var videoLookup: Map<String, VideoItem> = emptyMap()
    private var positionSaveJob: Job? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var volumeBeforeMute: Float = 1f

    init {
        PlaybackSessionHolder.session = mediaSession
        runCatching { application.startService(Intent(application, PlaybackSessionService::class.java)) }

        viewModelScope.launch {
            appearancePrefsRepository.state.collectLatest { appearance ->
                val startingUp = _uiState.value.currentVideo == null
                _uiState.value = _uiState.value.copy(
                    appearance = appearance,
                    playbackSpeed = if (startingUp) appearance.defaultPlaybackSpeed else _uiState.value.playbackSpeed
                )
            }
        }
        viewModelScope.launch {
            playbackPrefsRepository.state.collectLatest { prefs ->
                val startingUp = _uiState.value.currentVideo == null
                _uiState.value = _uiState.value.copy(
                    playbackPrefs = prefs,
                    aspectRatioMode = if (startingUp) prefs.defaultAspectRatio else _uiState.value.aspectRatioMode
                )
                applyAudioBoost(prefs.audioBoostEnabled, prefs.audioBoostLevel)
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
                    // Reached with repeat=OFF on the last item in the queue -- nothing
                    // for onMediaItemTransition to hand off to, so finalize state here.
                    _uiState.value.currentVideo?.let { video ->
                        viewModelScope.launch {
                            playbackStateRepository.setWatched(video.id, true)
                            playbackStateRepository.clearPosition(video.id)
                        }
                    }
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val previousVideo = _uiState.value.currentVideo
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO && previousVideo != null) {
                    viewModelScope.launch {
                        playbackStateRepository.setWatched(previousVideo.id, true)
                        playbackStateRepository.clearPosition(previousVideo.id)
                    }
                }

                val nextVideo = mediaItem?.mediaId?.let { videoLookup[it] } ?: return
                _uiState.value = _uiState.value.copy(
                    currentVideo = nextVideo,
                    durationMs = nextVideo.durationMs,
                    hasNext = player.hasNextMediaItem(),
                    hasPrevious = player.hasPreviousMediaItem(),
                    isHardwareDecoder = null,
                    frameRate = null,
                    error = null
                )

                // "Auto-play next" off means: land on the next item but don't start it --
                // the queue still advances (so shuffle/repeat-all keep working structurally),
                // it just waits for the person to tap play. A manual Next/Previous tap
                // (reason != AUTO) always plays, regardless of this setting.
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO && !_uiState.value.playbackPrefs.autoPlayNext) {
                    player.pause()
                }

                if (_uiState.value.playbackPrefs.resumePlaybackEnabled) {
                    viewModelScope.launch {
                        val resumeMs = playbackStateRepository.observeState(nextVideo.id).first().lastPositionMs
                        if (resumeMs > 0L) player.seekTo(resumeMs)
                    }
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
                    subtitleTracks = subtitleOptions,
                    frameRate = selectedVideoFrameRate(tracks)
                )
                _uiState.value.currentVideo?.let { video ->
                    viewModelScope.launch {
                        playbackStateRepository.recordSubtitleAvailability(video.id, subtitleOptions.isNotEmpty())
                    }
                }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _uiState.value = _uiState.value.copy(repeatMode = repeatMode.toAppRepeatMode())
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _uiState.value = _uiState.value.copy(shuffleEnabled = shuffleModeEnabled)
            }

            override fun onVolumeChanged(volume: Float) {
                if (volume > 0f) volumeBeforeMute = volume
                _uiState.value = _uiState.value.copy(isMuted = volume == 0f)
            }
        })

        // LoudnessEnhancer must be re-attached whenever ExoPlayer opens a new audio
        // session (e.g. on every new MediaItem), so audio boost survives track changes.
        // The decoder-name heuristic for the "HW" badge is best-effort: Android doesn't
        // expose a direct "is this hardware accelerated" API, so this checks whether the
        // active decoder's name looks like one of the known software fallbacks
        // (Google's software codecs) versus a vendor/hardware one.
        player.addAnalyticsListener(object : AnalyticsListener {
            override fun onAudioSessionIdChanged(eventTime: AnalyticsListener.EventTime, audioSessionId: Int) {
                recreateLoudnessEnhancer(audioSessionId)
            }

            override fun onVideoDecoderInitialized(
                eventTime: AnalyticsListener.EventTime,
                decoderName: String,
                initializedTimestampMs: Long,
                initializationDurationMs: Long
            ) {
                val isSoftware = decoderName.contains("google", ignoreCase = true) ||
                    decoderName.startsWith("c2.android", ignoreCase = true) ||
                    decoderName.startsWith("OMX.android", ignoreCase = true)
                _uiState.value = _uiState.value.copy(isHardwareDecoder = !isSoftware)
            }
        })
    }

    private fun selectedVideoFrameRate(tracks: Tracks): Float? {
        for (group in tracks.groups) {
            if (group.type != C.TRACK_TYPE_VIDEO) continue
            for (i in 0 until group.length) {
                if (group.isTrackSelected(i)) {
                    val rate = group.getTrackFormat(i).frameRate
                    if (rate > 0f) return rate
                }
            }
        }
        return null
    }

    /**
     * Loads the whole folder as a native ExoPlayer playlist (instead of manually tracking
     * an index and swapping single MediaItems), so previous/next, shuffle, and repeat are
     * all handled by ExoPlayer itself rather than reimplemented here.
     */
    fun openVideo(videoId: Long, bucketId: String) {
        viewModelScope.launch {
            val allVideos = mediaStoreRepository.loadAllVideos()
            var playlist = allVideos.filter { it.bucketId == bucketId }
            if (playlist.none { it.id == videoId }) {
                allVideos.firstOrNull { it.id == videoId }?.let { playlist = listOf(it) }
            }
            if (playlist.isEmpty()) return@launch

            videoLookup = playlist.associateBy { it.id.toString() }
            val startIndex = playlist.indexOfFirst { it.id == videoId }.coerceAtLeast(0)
            val startVideo = playlist[startIndex]

            val resumeEnabled = _uiState.value.playbackPrefs.resumePlaybackEnabled
            val resumeMs = if (resumeEnabled) {
                playbackStateRepository.observeState(startVideo.id).first().lastPositionMs
            } else {
                0L
            }

            val mediaItems = playlist.map { video ->
                MediaItem.Builder()
                    .setUri(video.contentUri)
                    .setMediaId(video.id.toString())
                    .build()
            }

            player.setMediaItems(mediaItems, startIndex, resumeMs)
            player.playbackParameters = player.playbackParameters.withSpeed(_uiState.value.playbackSpeed)
            player.prepare()
            player.playWhenReady = true

            _uiState.value = _uiState.value.copy(
                currentVideo = startVideo,
                error = null,
                hasNext = player.hasNextMediaItem(),
                hasPrevious = player.hasPreviousMediaItem(),
                durationMs = startVideo.durationMs,
                currentPositionMs = resumeMs,
                isHardwareDecoder = null,
                frameRate = null
            )
        }
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

    /** Configured double-tap/±seek duration, in milliseconds, from Settings (default 10s). */
    fun seekStepMs(): Long = _uiState.value.playbackPrefs.doubleTapSeekSeconds * 1000L

    fun skipToNext() {
        if (player.hasNextMediaItem()) {
            persistCurrentPosition()
            player.seekToNextMediaItem()
            player.play()
        }
    }

    fun skipToPrevious() {
        if (player.hasPreviousMediaItem()) {
            persistCurrentPosition()
            player.seekToPreviousMediaItem()
            player.play()
        } else {
            player.seekTo(0)
        }
    }

    fun cycleRepeatMode() {
        player.repeatMode = when (_uiState.value.repeatMode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_ONE
            RepeatMode.ONE -> Player.REPEAT_MODE_ALL
            RepeatMode.ALL -> Player.REPEAT_MODE_OFF
        }
    }

    fun toggleShuffle() {
        player.shuffleModeEnabled = !player.shuffleModeEnabled
    }

    fun toggleMute() {
        if (player.volume > 0f) {
            volumeBeforeMute = player.volume
            player.volume = 0f
        } else {
            player.volume = volumeBeforeMute.takeIf { it > 0f } ?: 1f
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        player.playbackParameters = player.playbackParameters.withSpeed(speed)
        _uiState.value = _uiState.value.copy(playbackSpeed = speed)
    }

    fun cycleAspectRatio() {
        val next = when (_uiState.value.aspectRatioMode) {
            AspectRatioMode.FIT -> AspectRatioMode.FILL
            AspectRatioMode.FILL -> AspectRatioMode.CROP
            AspectRatioMode.CROP -> AspectRatioMode.FIT
        }
        _uiState.value = _uiState.value.copy(aspectRatioMode = next)
    }

    fun selectAudioTrack(option: TrackOption) {
        trackSelector.parameters = trackSelector.parameters.buildUpon()
            .setOverrideForType(
                TrackSelectionOverride(
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
                TrackSelectionOverride(
                    player.currentTracks.groups[option.groupIndex].mediaTrackGroup,
                    option.trackIndex
                )
            )
            _uiState.value = _uiState.value.copy(subtitlesEnabled = true)
        }
        trackSelector.parameters = builder.build()
    }

    /**
     * Adds an external subtitle file (.srt/.vtt) picked by the person as a sidecar track
     * for the video currently playing, by rebuilding its MediaItem with a subtitle
     * configuration and re-seeking to where playback was.
     */
    fun loadExternalSubtitle(uri: android.net.Uri, displayName: String) {
        val video = _uiState.value.currentVideo ?: return
        val mimeType = when {
            displayName.endsWith(".srt", ignoreCase = true) -> "application/x-subrip"
            displayName.endsWith(".vtt", ignoreCase = true) -> "text/vtt"
            displayName.endsWith(".ssa", ignoreCase = true) || displayName.endsWith(".ass", ignoreCase = true) -> "text/x-ssa"
            else -> "application/x-subrip"
        }
        val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(uri)
            .setMimeType(mimeType)
            .setLanguage("ext")
            .setLabel(displayName)
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()

        val resumePosition = player.currentPosition
        val wasPlaying = player.isPlaying
        val currentMediaItem = player.currentMediaItem ?: return
        val rebuilt = currentMediaItem.buildUpon()
            .setSubtitleConfigurations(listOf(subtitleConfig))
            .build()

        player.replaceMediaItem(player.currentMediaItemIndex, rebuilt)
        player.seekTo(resumePosition)
        player.playWhenReady = wasPlaying
        _uiState.value = _uiState.value.copy(subtitlesEnabled = true)
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

    // --- Audio boost (MX Player-style loudness boost above 100%) ---------------

    private fun recreateLoudnessEnhancer(audioSessionId: Int) {
        loudnessEnhancer?.release()
        loudnessEnhancer = null
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) return
        loudnessEnhancer = runCatching { LoudnessEnhancer(audioSessionId) }.getOrNull()
        val prefs = _uiState.value.playbackPrefs
        applyAudioBoost(prefs.audioBoostEnabled, prefs.audioBoostLevel)
    }

    private fun applyAudioBoost(enabled: Boolean, level: Float) {
        val enhancer = loudnessEnhancer ?: return
        runCatching {
            // level is 0..100 from the Settings slider; LoudnessEnhancer takes millibels,
            // 2000mB (=20dB) is a sensible ceiling before clipping/distortion becomes obvious.
            enhancer.setTargetGain((level / 100f * 2000f).toInt())
            enhancer.enabled = enabled
        }
    }

    // --- Position persistence ----------------------------------------------------

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
        loudnessEnhancer?.release()
        if (PlaybackSessionHolder.session === mediaSession) {
            PlaybackSessionHolder.session = null
        }
        mediaSession.release()
        player.release()
        runCatching { application.stopService(Intent(application, PlaybackSessionService::class.java)) }
        super.onCleared()
    }

    class Factory(
        private val application: Application,
        private val mediaStoreRepository: MediaStoreRepository,
        private val playbackStateRepository: PlaybackStateRepository,
        private val appearancePrefsRepository: AppearancePrefsRepository,
        private val playbackPrefsRepository: PlaybackPrefsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return PlayerViewModel(
                application, mediaStoreRepository, playbackStateRepository, appearancePrefsRepository, playbackPrefsRepository
            ) as T
        }
    }
}

private fun Int.toAppRepeatMode(): RepeatMode = when (this) {
    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
    else -> RepeatMode.OFF
}
