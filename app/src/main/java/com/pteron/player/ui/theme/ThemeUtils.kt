package com.pteron.player.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.pteron.player.data.model.AppThemeMode
import com.pteron.player.data.model.FolderColor
import androidx.compose.ui.graphics.Color

/** Resolves the user's [AppThemeMode] choice against the current system setting. */
@Composable
fun resolveDarkTheme(mode: AppThemeMode): Boolean = when (mode) {
    AppThemeMode.LIGHT -> false
    AppThemeMode.DARK -> true
    AppThemeMode.SYSTEM -> isSystemInDarkTheme()
}

/** Picks the light or dark swatch for a [FolderColor] given the active theme. */
fun FolderColor.themedColor(isDarkTheme: Boolean): Color =
    if (isDarkTheme) dark else light
