package com.pteron.player.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pteron.player.data.model.AspectRatioMode
import com.pteron.player.data.prefs.AppFont
import com.pteron.player.data.prefs.AppTheme
import com.pteron.player.data.prefs.AppearancePrefsRepository
import com.pteron.player.data.prefs.AppearanceState
import com.pteron.player.data.prefs.AudioStateRepository
import com.pteron.player.data.prefs.MusicPrefsRepository
import com.pteron.player.data.prefs.MusicPrefsState
import com.pteron.player.data.prefs.NowPlayingStyle
import com.pteron.player.data.prefs.OrientationLock
import com.pteron.player.data.prefs.PlaybackPrefsRepository
import com.pteron.player.data.prefs.PlaybackPrefsState
import com.pteron.player.data.prefs.PlaybackStateRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val appearanceRepository: AppearancePrefsRepository,
    private val playbackPrefsRepository: PlaybackPrefsRepository,
    private val playbackStateRepository: PlaybackStateRepository,
    private val musicPrefsRepository: MusicPrefsRepository,
    private val audioStateRepository: AudioStateRepository
) : ViewModel() {

    val appearance: StateFlow<AppearanceState> = appearanceRepository.state.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), appearanceRepository.cachedThemeSettings().let {
            AppearanceState(
                theme = it.theme,
                canvasShade = it.canvasShade,
                cardShade = it.cardShade,
                folderShade = it.folderShade,
                font = it.font,
                fontScale = it.fontScale
            )
        }
    )
    val playbackPrefs: StateFlow<PlaybackPrefsState> = playbackPrefsRepository.state.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), PlaybackPrefsState()
    )

    val musicPrefs: StateFlow<MusicPrefsState> = musicPrefsRepository.state.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), MusicPrefsState()
    )

    // --- Appearance -------------------------------------------------------------

    fun setTheme(theme: AppTheme) = viewModelScope.launch { appearanceRepository.setTheme(theme) }
    fun setCanvasShade(value: Float) = viewModelScope.launch { appearanceRepository.setCanvasShade(value) }
    fun setCardShade(value: Float) = viewModelScope.launch { appearanceRepository.setCardShade(value) }
    fun setFolderShade(value: Float) = viewModelScope.launch { appearanceRepository.setFolderShade(value) }
    fun resetShades() = viewModelScope.launch { appearanceRepository.resetShades() }
    fun setFont(font: AppFont) = viewModelScope.launch { appearanceRepository.setFont(font) }
    fun setFontScale(scale: Float) = viewModelScope.launch { appearanceRepository.setFontScale(scale) }
    fun setVideoGridColumns(columns: Int) = viewModelScope.launch { appearanceRepository.setVideoGridColumns(columns) }
    fun setShowVideoCountBadge(value: Boolean) = viewModelScope.launch { appearanceRepository.setShowVideoCountBadge(value) }
    fun setShowFolderSizeBadge(value: Boolean) = viewModelScope.launch { appearanceRepository.setShowFolderSizeBadge(value) }
    fun setGestureSensitivity(value: Float) = viewModelScope.launch { appearanceRepository.setGestureSensitivity(value) }
    fun resetToDefaults() = viewModelScope.launch { appearanceRepository.resetToDefaults() }

    // --- Playback behavior (MX Player-style) -------------------------------------

    fun setResumePlaybackEnabled(value: Boolean) = viewModelScope.launch { playbackPrefsRepository.setResumePlaybackEnabled(value) }
    fun setAutoPlayNext(value: Boolean) = viewModelScope.launch { playbackPrefsRepository.setAutoPlayNext(value) }
    fun setBackgroundPlaybackEnabled(value: Boolean) = viewModelScope.launch { playbackPrefsRepository.setBackgroundPlaybackEnabled(value) }
    fun setDoubleTapSeekSeconds(seconds: Int) = viewModelScope.launch { playbackPrefsRepository.setDoubleTapSeekSeconds(seconds) }
    fun setAudioBoostEnabled(value: Boolean) = viewModelScope.launch { playbackPrefsRepository.setAudioBoostEnabled(value) }
    fun setAudioBoostLevel(value: Float) = viewModelScope.launch { playbackPrefsRepository.setAudioBoostLevel(value) }
    fun setSubtitleTextSize(sp: Float) = viewModelScope.launch { playbackPrefsRepository.setSubtitleTextSize(sp) }
    fun setKeepScreenOnWhilePlaying(value: Boolean) = viewModelScope.launch { playbackPrefsRepository.setKeepScreenOnWhilePlaying(value) }
    fun setOrientationLock(lock: OrientationLock) = viewModelScope.launch { playbackPrefsRepository.setOrientationLock(lock) }
    fun setDefaultAspectRatio(mode: AspectRatioMode) = viewModelScope.launch { playbackPrefsRepository.setDefaultAspectRatio(mode) }
    fun setControlAutoHideSeconds(seconds: Int) = viewModelScope.launch { playbackPrefsRepository.setControlAutoHideSeconds(seconds) }

    // --- Music player -------------------------------------------------------------

    fun setSkipSilence(value: Boolean) = viewModelScope.launch { musicPrefsRepository.setSkipSilence(value) }
    fun setLoudnessBoostEnabled(value: Boolean) = viewModelScope.launch { musicPrefsRepository.setLoudnessBoostEnabled(value) }
    fun setLoudnessBoostLevel(value: Float) = viewModelScope.launch { musicPrefsRepository.setLoudnessBoostLevel(value) }
    fun setBassBoostLevel(value: Float) = viewModelScope.launch { musicPrefsRepository.setBassBoostLevel(value) }
    fun setVirtualizerLevel(value: Float) = viewModelScope.launch { musicPrefsRepository.setVirtualizerLevel(value) }
    fun setMinTrackSeconds(value: Int) = viewModelScope.launch { musicPrefsRepository.setMinTrackSeconds(value) }
    fun setPauseOnHeadphonesUnplugged(value: Boolean) = viewModelScope.launch { musicPrefsRepository.setPauseOnHeadphonesUnplugged(value) }
    fun setKeepScreenOnInNowPlaying(value: Boolean) = viewModelScope.launch { musicPrefsRepository.setKeepScreenOnInNowPlaying(value) }
    fun setOpenNowPlayingOnPlay(value: Boolean) = viewModelScope.launch { musicPrefsRepository.setOpenNowPlayingOnPlay(value) }
    fun setNowPlayingStyle(style: NowPlayingStyle) = viewModelScope.launch { musicPrefsRepository.setNowPlayingStyle(style) }
    fun clearRecentlyPlayed() = viewModelScope.launch { audioStateRepository.clearRecent() }

    fun clearWatchHistory() = viewModelScope.launch { playbackStateRepository.clearAll() }

    fun toggleDarkMode() = viewModelScope.launch { appearanceRepository.toggleDarkMode() }

    class Factory(
        private val appearanceRepository: AppearancePrefsRepository,
        private val playbackPrefsRepository: PlaybackPrefsRepository,
        private val playbackStateRepository: PlaybackStateRepository,
        private val musicPrefsRepository: MusicPrefsRepository,
        private val audioStateRepository: AudioStateRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(
                appearanceRepository, playbackPrefsRepository, playbackStateRepository,
                musicPrefsRepository, audioStateRepository
            ) as T
    }
}
