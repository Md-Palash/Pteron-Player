package com.pteron.player.ui.player

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.SystemClock
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import com.pteron.player.MainActivity
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
import com.pteron.player.util.PipController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

/**
 * Everything that changes rarely (a few times per video). The playback position, which changes
 * every second, lives in [PlayerProgress] so that a tick never recomposes the whole screen.
 */
data class PlayerUiState(
    val currentVideo: VideoItem? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = true,
    val durationMs: Long = 0L,
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

/** The fast-changing part of the player state (updated about once per second while visible). */
data class PlayerProgress(
    val positionMs: Long = 0L,
    val bufferedPercentage: Int = 0
)

/** Id of a video opened from another app: it has no MediaStore row, so nothing is persisted for it. */
private const val EXTERNAL_VIDEO_ID = -1L

/** How often the playback position is written to disk while playing. */
private const val POSITION_SAVE_INTERVAL_MS = 5_000L

/** "Previous" restarts the current video when it's been playing longer than this. */
private const val RESTART_THRESHOLD_MS = 3_000L

class PlayerViewModel(
    private val application: Application,
    private val mediaStoreRepository: MediaStoreRepository,
    private val playbackStateRepository: PlaybackStateRepository,
    private val appearancePrefsRepository: AppearancePrefsRepository,
    private val playbackPrefsRepository: PlaybackPrefsRepository
) : AndroidViewModel(application) {

    private val trackSelector = DefaultTrackSelector(application)

    // Local files can be read almost instantly, so ExoPlayer's network-oriented defaults
    // (50s of look-ahead, up to ~128 MB of compressed data held in RAM) are wasteful here.
    // A short look-ahead plus a hard byte cap keeps memory flat, even for 4K high-bitrate files.
    private val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            /* minBufferMs = */ 8_000,
            /* maxBufferMs = */ 15_000,
            /* bufferForPlaybackMs = */ 500,
            /* bufferForPlaybackAfterRebufferMs = */ 1_500
        )
        .setTargetBufferBytes(32 * 1024 * 1024)
        .setPrioritizeTimeOverSizeThresholds(false)
        .build()

    // If the preferred decoder fails to start (odd HDR/Dolby Vision streams, busy codecs),
    // fall back to the next one instead of surfacing a playback error.
    private val renderersFactory = DefaultRenderersFactory(application)
        .setEnableDecoderFallback(true)

    val player: ExoPlayer = ExoPlayer.Builder(application, renderersFactory)
        .setTrackSelector(trackSelector)
        .setLoadControl(loadControl)
        // true = ExoPlayer manages audio focus: pauses for calls / other media apps and resumes after.
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            /* handleAudioFocus = */ true
        )
        // Pause when headphones are unplugged / Bluetooth disconnects.
        .setHandleAudioBecomingNoisy(true)
        .build()

    // Exposes this player to the system (notification + lock screen controls,
    // headphone/Bluetooth media-button routing) via a MediaSessionService that
    // shares this in-process session rather than owning a separate player --
    // see PlaybackSessionHolder for why that's safe.
    private val mediaSession: MediaSession = MediaSession.Builder(application, player)
        // Tapping the media notification returns to the app.
        .setSessionActivity(
            PendingIntent.getActivity(
                application,
                0,
                Intent(application, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        .build()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _progress = MutableStateFlow(PlayerProgress())
    val progress: StateFlow<PlayerProgress> = _progress.asStateFlow()

    /** mediaId (VideoItem.id.toString()) -> VideoItem, rebuilt each time a playlist is opened. */
    private var videoLookup: Map<String, VideoItem> = emptyMap()
    private var positionTickerJob: Job? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var audioSessionId: Int = C.AUDIO_SESSION_ID_UNSET
    private var volumeBeforeMute: Float = 1f

    /** Identifies what was last opened, so a re-composition can't reload the queue and restart playback. */
    private var openedKey: String? = null

    /** False while the app is minimized / the screen is off: skip UI ticks and drop video decoding. */
    @Volatile
    private var uiActive: Boolean = true
    private var lastSavedAtMs: Long = 0L

    // Outlives viewModelScope, which is already cancelled by the time onCleared() runs --
    // needed so the final position is really written when the person leaves the player.
    private val persistScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
                    saveProgress(force = true)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val state = _uiState.value
                val duration = player.duration
                _uiState.value = state.copy(
                    isBuffering = playbackState == Player.STATE_BUFFERING,
                    // Duration is unknown while buffering; keep the last known value instead of 0.
                    durationMs = if (duration > 0L) duration else state.durationMs
                )
                if (playbackState == Player.STATE_ENDED) {
                    // Reached with repeat=OFF on the last item in the queue -- nothing
                    // for onMediaItemTransition to hand off to, so finalize state here.
                    state.currentVideo?.takeIf { it.id != EXTERNAL_VIDEO_ID }?.let { video ->
                        viewModelScope.launch {
                            playbackStateRepository.setWatched(video.id, true)
                            playbackStateRepository.clearPosition(video.id)
                        }
                    }
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                // Seeks while paused don't tick, so refresh the seek bar right away.
                publishProgress()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val previousVideo = _uiState.value.currentVideo
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO &&
                    previousVideo != null && previousVideo.id != EXTERNAL_VIDEO_ID
                ) {
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

                // Resume only when moving to a *different* item. The first item's resume position is
                // already applied by setMediaItems(), and REPEAT / PLAYLIST_CHANGED (e.g. adding a
                // subtitle) must never jump back to the last saved position.
                val movedToOtherItem = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                    reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK
                if (movedToOtherItem && nextVideo.id != EXTERNAL_VIDEO_ID &&
                    _uiState.value.playbackPrefs.resumePlaybackEnabled
                ) {
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
                    frameRate = selectedVideoFrameRate(tracks) ?: _uiState.value.frameRate
                )
                _uiState.value.currentVideo?.takeIf { it.id != EXTERNAL_VIDEO_ID }?.let { video ->
                    viewModelScope.launch {
                        playbackStateRepository.recordSubtitleAvailability(video.id, subtitleOptions.isNotEmpty())
                    }
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                // The real, rotation-aware display size -- MediaStore's width/height can be the
                // un-rotated size for portrait phone videos, and external videos have none at all.
                if (videoSize.width > 0 && videoSize.height > 0) {
                    PipController.videoAspectRatio.value =
                        videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
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
                onAudioSessionChanged(audioSessionId)
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
     *
     * Calling this again with the same arguments is a no-op, so a re-composition of the
     * player screen can never reload the queue or restart the video.
     */
    fun openVideo(videoId: Long, bucketId: String, shuffle: Boolean = false) {
        val key = "lib|$videoId|$bucketId|$shuffle"
        if (openedKey == key) return
        openedKey = key

        viewModelScope.launch {
            val allVideos = mediaStoreRepository.loadAllVideos()
            var playlist = allVideos.filter { it.bucketId == bucketId }
            if (playlist.none { it.id == videoId }) {
                allVideos.firstOrNull { it.id == videoId }?.let { playlist = listOf(it) }
            }
            if (playlist.isEmpty()) {
                openedKey = null
                return@launch
            }

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

            player.shuffleModeEnabled = shuffle
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
                isHardwareDecoder = null,
                frameRate = null
            )
            _progress.value = PlayerProgress(positionMs = resumeMs)
        }
    }

    /**
     * Plays a single video handed over by another app (ACTION_VIEW). It isn't part of the
     * MediaStore library, so there is no folder queue and no resume / watched / favorite state.
     */
    fun openExternal(uri: Uri) {
        val key = "ext|$uri"
        if (openedKey == key) return
        openedKey = key

        viewModelScope.launch {
            val name = withContext(Dispatchers.IO) { queryDisplayName(uri) }
                ?: uri.lastPathSegment
                ?: "Video"
            val video = VideoItem(
                id = EXTERNAL_VIDEO_ID,
                contentUri = uri.toString(),
                displayName = name,
                bucketId = "",
                durationMs = 0L,
                sizeBytes = 0L,
                dateAddedSeconds = 0L,
                width = 0,
                height = 0,
                mimeType = ""
            )
            videoLookup = mapOf(video.id.toString() to video)

            player.shuffleModeEnabled = false
            player.setMediaItem(
                MediaItem.Builder()
                    .setUri(uri)
                    .setMediaId(video.id.toString())
                    .build()
            )
            player.playbackParameters = player.playbackParameters.withSpeed(_uiState.value.playbackSpeed)
            player.prepare()
            player.playWhenReady = true

            _uiState.value = _uiState.value.copy(
                currentVideo = video,
                error = null,
                hasNext = false,
                hasPrevious = false,
                durationMs = 0L,
                isHardwareDecoder = null,
                frameRate = null
            )
            _progress.value = PlayerProgress()
        }
    }

    fun playPause() {
        player.playWhenReady = !player.playWhenReady
    }

    fun pause() {
        player.pause()
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
            saveProgress(force = true)
            player.seekToNextMediaItem()
            player.play()
        }
    }

    fun skipToPrevious() {
        // Standard player behavior: a few seconds in, "previous" restarts the current video;
        // only near the start does it go to the previous one.
        if (player.hasPreviousMediaItem() && player.currentPosition <= RESTART_THRESHOLD_MS) {
            saveProgress(force = true)
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
    fun loadExternalSubtitle(uri: Uri, displayName: String) {
        _uiState.value.currentVideo ?: return
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
        if (video.id == EXTERNAL_VIDEO_ID) return
        viewModelScope.launch { playbackStateRepository.setFavorite(video.id, !video.isFavorite) }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Called by the player screen when it becomes visible / hidden (app minimized, screen off).
     *
     * While nothing is on screen there is no point decoding video frames: disabling the video
     * track type releases the video decoder (RAM, CPU, battery) while audio keeps playing through
     * the media notification. Picture-in-Picture counts as visible, so it is unaffected.
     */
    fun onUiVisibilityChanged(visible: Boolean) {
        if (uiActive == visible) return
        uiActive = visible
        if (!visible) saveProgress(force = true)
        trackSelector.parameters = trackSelector.parameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, !visible)
            .build()
        if (visible) publishProgress()
    }

    private fun queryDisplayName(uri: Uri): String? = runCatching {
        application.contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    }.getOrNull()

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

    private fun onAudioSessionChanged(newAudioSessionId: Int) {
        audioSessionId = newAudioSessionId
        loudnessEnhancer?.release()
        loudnessEnhancer = null
        val prefs = _uiState.value.playbackPrefs
        applyAudioBoost(prefs.audioBoostEnabled, prefs.audioBoostLevel)
    }

    private fun applyAudioBoost(enabled: Boolean, level: Float) {
        if (!enabled) {
            // Don't keep a native audio effect alive when the feature is off.
            loudnessEnhancer?.release()
            loudnessEnhancer = null
            return
        }
        if (loudnessEnhancer == null && audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
            loudnessEnhancer = runCatching { LoudnessEnhancer(audioSessionId) }.getOrNull()
        }
        val enhancer = loudnessEnhancer ?: return
        runCatching {
            // level is 0..100 from the Settings slider; LoudnessEnhancer takes millibels,
            // 2000mB (=20dB) is a sensible ceiling before clipping/distortion becomes obvious.
            enhancer.setTargetGain((level / 100f * 2000f).toInt())
            enhancer.enabled = true
        }
    }

    // --- Progress + position persistence -------------------------------------------

    /** Pushes the current position to the UI. Cheap: only the seek bar / overlay observe it. */
    private fun publishProgress() {
        _progress.value = PlayerProgress(
            positionMs = player.currentPosition.coerceAtLeast(0L),
            bufferedPercentage = player.bufferedPercentage
        )
        val duration = player.duration
        if (duration > 0L && duration != _uiState.value.durationMs) {
            _uiState.value = _uiState.value.copy(durationMs = duration)
        }
    }

    private fun startPositionTicker() {
        stopPositionTicker()
        lastSavedAtMs = SystemClock.elapsedRealtime()
        positionTickerJob = viewModelScope.launch {
            while (true) {
                val visible = uiActive
                if (visible) publishProgress()
                saveProgress(force = false)
                // Nothing to redraw while minimized, so wake up far less often (the periodic
                // save is just a safety net in case the process is killed).
                delay(if (visible) 1_000L else 5_000L)
            }
        }
    }

    private fun stopPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = null
    }

    /**
     * Writes the position to disk. Periodic saves are throttled to [POSITION_SAVE_INTERVAL_MS]
     * (it used to be every second, i.e. a DataStore write per second for the whole video);
     * [force] is used for the moments that matter -- pause, skip, minimize, leave.
     */
    private fun saveProgress(force: Boolean) {
        val video = _uiState.value.currentVideo ?: return
        if (video.id == EXTERNAL_VIDEO_ID) return
        // Once ended, ENDED handling clears the saved position; don't resurrect it here.
        if (player.playbackState == Player.STATE_ENDED) return
        val position = player.currentPosition
        if (position <= 0L) return
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastSavedAtMs < POSITION_SAVE_INTERVAL_MS) return
        lastSavedAtMs = now
        val duration = player.duration.coerceAtLeast(0L)
        viewModelScope.launch {
            playbackStateRepository.savePosition(video.id, position, duration)
        }
    }

    override fun onCleared() {
        // Final save on leaving the player. Runs on persistScope because viewModelScope is
        // already cancelled at this point (a launch there would silently never execute).
        val video = _uiState.value.currentVideo
        if (video != null && video.id != EXTERNAL_VIDEO_ID && player.playbackState != Player.STATE_ENDED) {
            val position = player.currentPosition
            val duration = player.duration.coerceAtLeast(0L)
            if (position > 0L) {
                persistScope.launch { playbackStateRepository.savePosition(video.id, position, duration) }
            }
        }
        stopPositionTicker()
        loudnessEnhancer?.release()
        loudnessEnhancer = null
        // Only tear down the shared session/service if a newer player hasn't already taken over
        // (opening another video replaces this screen; its ViewModel is created first).
        val ownsSession = PlaybackSessionHolder.session === mediaSession
        if (ownsSession) PlaybackSessionHolder.session = null
        mediaSession.release()
        player.release()
        if (ownsSession) {
            runCatching { application.stopService(Intent(application, PlaybackSessionService::class.java)) }
        }
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
