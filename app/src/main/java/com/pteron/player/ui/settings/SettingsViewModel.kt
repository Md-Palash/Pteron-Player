package com.pteron.player.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pteron.player.data.prefs.AccentColor
import com.pteron.player.data.prefs.AppearancePrefsRepository
import com.pteron.player.data.prefs.AppearanceState
import com.pteron.player.data.prefs.BackgroundTheme
import com.pteron.player.data.prefs.FolderTone
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: AppearancePrefsRepository
) : ViewModel() {

    val appearance: StateFlow<AppearanceState> = repository.state.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), AppearanceState()
    )

    fun setBackgroundTheme(theme: BackgroundTheme) = viewModelScope.launch { repository.setBackgroundTheme(theme) }
    fun setAccentColor(color: AccentColor) = viewModelScope.launch { repository.setAccentColor(color) }
    fun setFolderTone(tone: FolderTone) = viewModelScope.launch { repository.setFolderTone(tone) }
    fun setShowVideoCountBadge(value: Boolean) = viewModelScope.launch { repository.setShowVideoCountBadge(value) }
    fun setShowFolderSizeBadge(value: Boolean) = viewModelScope.launch { repository.setShowFolderSizeBadge(value) }
    fun setMatchControlsToAccent(value: Boolean) = viewModelScope.launch { repository.setMatchControlsToAccent(value) }
    fun setOledPureBlackControls(value: Boolean) = viewModelScope.launch { repository.setOledPureBlackControls(value) }
    fun setGestureSensitivity(value: Float) = viewModelScope.launch { repository.setGestureSensitivity(value) }
    fun resetToDefaults() = viewModelScope.launch { repository.resetToDefaults() }

    class Factory(private val repository: AppearancePrefsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(repository) as T
    }
}
