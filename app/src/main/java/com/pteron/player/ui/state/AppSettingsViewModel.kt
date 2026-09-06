package com.pteron.player.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.pteron.player.data.model.AppThemeMode
import com.pteron.player.data.model.FolderColor

/**
 * Holds Stage 1 appearance settings in memory for the lifetime of the
 * process. There is no persistence yet — values simply live here as
 * Compose state and reset on process death, which is expected for
 * this stage.
 *
 * Stage 2+: back this with DataStore. The public surface (two
 * properties + two setters) is deliberately small so a persistence
 * layer can slot in behind it without touching call sites.
 */
class AppSettingsViewModel : ViewModel() {

    var themeMode by mutableStateOf(AppThemeMode.SYSTEM)
        private set

    var defaultFolderColor by mutableStateOf(FolderColor.Default)
        private set

    fun updateThemeMode(mode: AppThemeMode) { themeMode = mode }

    fun updateFolderColor(color: FolderColor) { defaultFolderColor = color }
}
