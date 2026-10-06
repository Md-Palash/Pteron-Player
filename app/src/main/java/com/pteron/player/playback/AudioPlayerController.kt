package com.pteron.player.playback

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.media.audiofx.BassBoost
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.session.MediaSession
import com.pteron.player.MainActivity
import com.pteron.player.data.model.AudioItem
import com.pteron.player.data.prefs.AudioStateRepository
import com.pteron.player.data.prefs.MusicPrefsRepository
import com.pteron.player.data.prefs.MusicPrefsState
import com.pteron.player.ui.player.RepeatMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** What the mini player and the Now Playing screen show. The position lives in [AudioPlayerController.positionMs]. */
data class AudioPlayerState(
    val current: AudioItem? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.OFF,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false
)

/**
 * The music player: one ExoPlayer for the whole app process, created the first time the Audio tab
 * is opened (see PteronApp.audioController). It is separate from the video player's ExoPlayer, and
 * both use audio focus, so starting one pauses the other.
 *
 * Effects (bass boost, loudness, surround) are re-attached whenever ExoPlayer opens a new audio
 * session, and every Settings > Music Player value is applied live through [MusicPrefsRepository].
 */
class AudioPlayerController(
    private val application: Application,
    private val stateRepository: AudioStateRepository,
    prefsRepository: MusicPrefsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val player: ExoPlayer = ExoPlayer.Builder(application)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            /* handleAudioFocus = */ true
        )
        .setHandleAudioBecomingNoisy(true)
        .build()
        .apply { setWakeMode(C.WAKE_MODE_LOCAL) }

    private val session: MediaSession = MediaSession.Builder(application, player)
        .setId("pteron_audio")
        .setSessionActivity(
            PendingIntent.getActivity(
                application,
                1,
                Intent(application, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        .build()

    private val _state = MutableStateFlow(AudioPlayerState())
    val state: StateFlow<AudioPlayerState> = _state.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    /** mediaId (AudioItem.id.toString()) -> song, for every item that has ever been queued. */
    private val lookup = HashMap<String, AudioItem>()
    private var ticker: Job? = null

    private var prefs = MusicPrefsState()
    private var audioSessionId = C.AUDIO_SESSION_ID_UNSET
    private var loudness: LoudnessEnhancer? = null
    private var bass: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    init {
        AudioSessionHolder.session = session

        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) = publish()

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) startTicker() else stopTicker()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItem?.mediaId?.toLongOrNull()?.let { id ->
                    scope.launch { stateRepository.recordPlayed(id) }
                }
            }
        })

        // Effects must follow ExoPlayer's audio session, which changes with the audio format.
        player.addAnalyticsListener(object : AnalyticsListener {
            override fun onAudioSessionIdChanged(eventTime: AnalyticsListener.EventTime, audioSessionId: Int) {
                this@AudioPlayerController.audioSessionId = audioSessionId
                releaseEffects()
                applyEffects()
            }
        })

        scope.launch {
            prefsRepository.state.collect { newPrefs ->
                prefs = newPrefs
                player.skipSilenceEnabled = newPrefs.skipSilence
                player.setHandleAudioBecomingNoisy(newPrefs.pauseOnHeadphonesUnplugged)
                applyEffects()
            }
        }
    }

    // --- Commands ----------------------------------------------------------------------------------

    /** Replaces the queue with [songs] and starts playing at [startIndex]. */
    fun playQueue(songs: List<AudioItem>, startIndex: Int, shuffle: Boolean = false) {
        if (songs.isEmpty()) return
        runCatching { application.startService(Intent(application, AudioSessionService::class.java)) }
        songs.forEach { lookup[it.id.toString()] = it }
        player.shuffleModeEnabled = shuffle
        player.setMediaItems(songs.map(::toMediaItem), startIndex.coerceIn(0, songs.lastIndex), 0L)
        player.prepare()
        player.play()
    }

    /** Inserts [song] right after the one that is playing. */
    fun playNext(song: AudioItem) {
        if (player.mediaItemCount == 0) {
            playQueue(listOf(song), 0)
            return
        }
        lookup[song.id.toString()] = song
        player.addMediaItem(player.currentMediaItemIndex + 1, toMediaItem(song))
    }

    fun togglePlayPause() {
        when {
            player.playbackState == Player.STATE_ENDED -> {
                player.seekToDefaultPosition()
                player.play()
            }
            player.playWhenReady -> player.pause()
            else -> player.play()
        }
    }

    fun pause() = player.pause()

    fun next() {
        if (player.hasNextMediaItem()) player.seekToNextMediaItem()
    }

    /** A few seconds in, "previous" restarts the song; near the start it goes to the one before. */
    fun previous() {
        if (player.hasPreviousMediaItem() && player.currentPosition <= 3_000L) {
            player.seekToPreviousMediaItem()
        } else {
            player.seekTo(0)
            _positionMs.value = 0L
        }
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0L))
        _positionMs.value = positionMs.coerceAtLeast(0L)
    }

    fun toggleShuffle() {
        player.shuffleModeEnabled = !player.shuffleModeEnabled
    }

    fun cycleRepeat() {
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    // --- State -------------------------------------------------------------------------------------

    private fun publish() {
        val current = player.currentMediaItem?.mediaId?.let { lookup[it] }
        val duration = player.duration
        _state.value = AudioPlayerState(
            current = current,
            isPlaying = player.isPlaying,
            isBuffering = player.playbackState == Player.STATE_BUFFERING,
            durationMs = if (duration > 0L) duration else current?.durationMs ?: 0L,
            shuffle = player.shuffleModeEnabled,
            repeat = when (player.repeatMode) {
                Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                else -> RepeatMode.OFF
            },
            hasNext = player.hasNextMediaItem(),
            hasPrevious = player.hasPreviousMediaItem()
        )
        if (!player.isPlaying) _positionMs.value = player.currentPosition.coerceAtLeast(0L)
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (true) {
                _positionMs.value = player.currentPosition.coerceAtLeast(0L)
                delay(500)
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    private fun toMediaItem(song: AudioItem): MediaItem =
        MediaItem.Builder()
            .setUri(song.contentUri)
            .setMediaId(song.id.toString())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setArtworkUri(Uri.parse(song.albumArtUri))
                    .build()
            )
            .build()

    // --- Audio effects -----------------------------------------------------------------------------

    private fun releaseEffects() {
        loudness?.release(); loudness = null
        bass?.release(); bass = null
        virtualizer?.release(); virtualizer = null
    }

    private fun applyEffects() {
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) return

        if (prefs.loudnessBoostEnabled && prefs.loudnessBoostLevel > 0f) {
            if (loudness == null) loudness = runCatching { LoudnessEnhancer(audioSessionId) }.getOrNull()
            runCatching {
                // 0..100 -> 0..2000 mB (20 dB), a ceiling before distortion gets obvious.
                loudness?.setTargetGain((prefs.loudnessBoostLevel / 100f * 2000f).toInt())
                loudness?.enabled = true
            }
        } else {
            loudness?.release(); loudness = null
        }

        if (prefs.bassBoostLevel > 0f) {
            if (bass == null) bass = runCatching { BassBoost(0, audioSessionId) }.getOrNull()
            runCatching {
                bass?.setStrength((prefs.bassBoostLevel * 10f).toInt().coerceIn(0, 1000).toShort())
                bass?.enabled = true
            }
        } else {
            bass?.release(); bass = null
        }

        if (prefs.virtualizerLevel > 0f) {
            if (virtualizer == null) virtualizer = runCatching { Virtualizer(0, audioSessionId) }.getOrNull()
            runCatching {
                virtualizer?.setStrength((prefs.virtualizerLevel * 10f).toInt().coerceIn(0, 1000).toShort())
                virtualizer?.enabled = true
            }
        } else {
            virtualizer?.release(); virtualizer = null
        }
    }
}
