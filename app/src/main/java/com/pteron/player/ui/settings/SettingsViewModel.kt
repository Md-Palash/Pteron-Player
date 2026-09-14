package com.pteron.player.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pteron.player.data.model.AspectRatioMode
import com.pteron.player.data.prefs.AccentColor
import com.pteron.player.data.prefs.AppearancePrefsRepository
import com.pteron.player.data.prefs.AppearanceState
import com.pteron.player.data.prefs.BackgroundTheme
import com.pteron.player.data.prefs.FolderTone
import com.pteron.player.data.prefs.OrientationLock
import com.pteron.player.data.prefs.PlaybackPrefsRepository
import com.pteron.player.data.prefs.PlaybackPrefsState
import com.pteron.player.data.prefs.PlaybackStateRepository
import com.pteron.player.data.prefs.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val appearanceRepository: AppearancePrefsRepository,
    private val playbackPrefsRepository: PlaybackPrefsRepository,
    private val playbackStateRepository: PlaybackStateRepository
) : ViewModel() {

    val appearance: StateFlow<AppearanceState> = appearanceRepository.state.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), AppearanceState()
    )
    val playbackPrefs: StateFlow<PlaybackPrefsState> = playbackPrefsRepository.state.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), PlaybackPrefsState()
    )

    // --- Appearance -------------------------------------------------------------

    fun setBackgroundTheme(theme: BackgroundTheme) = viewModelScope.launch { appearanceRepository.setBackgroundTheme(theme) }
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { appearanceRepository.setThemeMode(mode) }
    fun setAccentColor(color: AccentColor) = viewModelScope.launch { appearanceRepository.setAccentColor(color) }
    fun setFolderTone(tone: FolderTone) = viewModelScope.launch { appearanceRepository.setFolderTone(tone) }
    fun setShowVideoCountBadge(value: Boolean) = viewModelScope.launch { appearanceRepository.setShowVideoCountBadge(value) }
    fun setShowFolderSizeBadge(value: Boolean) = viewModelScope.launch { appearanceRepository.setShowFolderSizeBadge(value) }
    fun setMatchControlsToAccent(value: Boolean) = viewModelScope.launch { appearanceRepository.setMatchControlsToAccent(value) }
    fun setOledPureBlackControls(value: Boolean) = viewModelScope.launch { appearanceRepository.setOledPureBlackControls(value) }
    fun setGestureSensitivity(value: Float) = viewModelScope.launch { appearanceRepository.setGestureSensitivity(value) }
    fun resetToDefaults() = viewModelScope.launch { appearanceRepository.resetToDefaults() }

    // --- Playback behavior (MX Player-style) -------------------------------------

    fun setResumePlaybackEnabled(value: Boolean) = viewModelScope.launch { playbackPrefsRepository.setResumePlaybackEnabled(value) }
    fun setAutoPlayNext(value: Boolean) = viewModelScope.launch { playbackPrefsRepository.setAutoPlayNext(value) }
    fun setDoubleTapSeekSeconds(seconds: Int) = viewModelScope.launch { playbackPrefsRepository.setDoubleTapSeekSeconds(seconds) }
    fun setAudioBoostEnabled(value: Boolean) = viewModelScope.launch { playbackPrefsRepository.setAudioBoostEnabled(value) }
    fun setAudioBoostLevel(value: Float) = viewModelScope.launch { playbackPrefsRepository.setAudioBoostLevel(value) }
    fun setSubtitleTextSize(sp: Float) = viewModelScope.launch { playbackPrefsRepository.setSubtitleTextSize(sp) }
    fun setKeepScreenOnWhilePlaying(value: Boolean) = viewModelScope.launch { playbackPrefsRepository.setKeepScreenOnWhilePlaying(value) }
    fun setOrientationLock(lock: OrientationLock) = viewModelScope.launch { playbackPrefsRepository.setOrientationLock(lock) }
    fun setDefaultAspectRatio(mode: AspectRatioMode) = viewModelScope.launch { playbackPrefsRepository.setDefaultAspectRatio(mode) }
    fun setControlAutoHideSeconds(seconds: Int) = viewModelScope.launch { playbackPrefsRepository.setControlAutoHideSeconds(seconds) }

    fun clearWatchHistory() = viewModelScope.launch { playbackStateRepository.clearAll() }

    class Factory(
        private val appearanceRepository: AppearancePrefsRepository,
        private val playbackPrefsRepository: PlaybackPrefsRepository,
        private val playbackStateRepository: PlaybackStateRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(appearanceRepository, playbackPrefsRepository, playbackStateRepository) as T
    }
}
