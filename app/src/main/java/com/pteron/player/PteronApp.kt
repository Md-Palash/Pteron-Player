package com.pteron.player

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pteron.player.navigation.PteronNavHost
import com.pteron.player.ui.state.AppSettingsViewModel
import com.pteron.player.ui.theme.PteronTheme
import com.pteron.player.ui.theme.resolveDarkTheme

@Composable
fun PteronApp() {
    val settingsViewModel: AppSettingsViewModel = viewModel()
    val isDarkTheme = resolveDarkTheme(settingsViewModel.themeMode)

    PteronTheme(themeMode = settingsViewModel.themeMode) {
        PteronNavHost(
            settingsViewModel = settingsViewModel,
            isDarkTheme = isDarkTheme
        )
    }
}
