package com.pteron.player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.pteron.player.data.model.AppThemeMode

private val LightColors = lightColorScheme(
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnBackground,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    primary = LightOnBackground,
    onPrimary = LightSurface
)

private val DarkColors = darkColorScheme(
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnBackground,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    primary = DarkOnBackground,
    onPrimary = DarkSurface
)

@Composable
fun PteronTheme(
    themeMode: AppThemeMode,
    content: @Composable () -> Unit
) {
    val useDarkTheme = resolveDarkTheme(themeMode)
    val colors = if (useDarkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = PteronTypography,
        shapes = PteronShapes,
        content = content
    )
}
