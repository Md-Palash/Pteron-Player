package com.pteron.player.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.pteron.player.data.prefs.AccentColor
import com.pteron.player.data.prefs.AppearanceState
import com.pteron.player.data.prefs.BackgroundTheme
import com.pteron.player.data.prefs.FolderTone

/** Exposes the raw appearance selections (accent hex, folder tone) to leaf composables
 *  that need them directly, e.g. folder cards and the player's scrub bar. */
val LocalAppearance = staticCompositionLocalOf { AppearanceState() }

private fun hex(hex: String) = Color(android.graphics.Color.parseColor(hex))

@Composable
fun PteronTheme(
    appearance: AppearanceState = AppearanceState(),
    content: @Composable () -> Unit
) {
    val accent = hex(appearance.accentColor.hex)
    val isDark = appearance.backgroundTheme.isDark
    val backgroundColor = hex(appearance.backgroundTheme.backgroundHex)

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = accent,
            onPrimary = Color.White,
            primaryContainer = accent.copy(alpha = 0.85f),
            onPrimaryContainer = Color.White,
            secondary = DarkSecondary,
            onSecondary = DarkOnSecondary,
            secondaryContainer = DarkSecondaryContainer,
            onSecondaryContainer = DarkOnSecondaryContainer,
            tertiary = DarkTertiary,
            onTertiary = DarkOnTertiary,
            tertiaryContainer = DarkTertiaryContainer,
            onTertiaryContainer = DarkOnTertiaryContainer,
            background = backgroundColor,
            onBackground = DarkOnBackground,
            surface = backgroundColor,
            onSurface = DarkOnSurface,
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = DarkOnSurfaceVariant,
            outline = DarkOutline,
            outlineVariant = DarkOutlineVariant,
            surfaceContainerLowest = DarkSurfaceContainerLowest,
            surfaceContainerLow = DarkSurfaceContainerLow,
            surfaceContainer = DarkSurfaceContainer,
            surfaceContainerHigh = DarkSurfaceContainerHigh,
            surfaceContainerHighest = DarkSurfaceContainerHighest,
            inverseSurface = DarkInverseSurface,
            inverseOnSurface = DarkInverseOnSurface,
            inversePrimary = DarkInversePrimary,
            error = LightError,
            onError = LightOnError,
            errorContainer = LightErrorContainer,
            onErrorContainer = LightOnErrorContainer
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            primaryContainer = accent.copy(alpha = 0.85f),
            onPrimaryContainer = Color.White,
            secondary = LightSecondary,
            onSecondary = LightOnSecondary,
            secondaryContainer = LightSecondaryContainer,
            onSecondaryContainer = LightOnSecondaryContainer,
            tertiary = LightTertiary,
            onTertiary = LightOnTertiary,
            tertiaryContainer = LightTertiaryContainer,
            onTertiaryContainer = LightOnTertiaryContainer,
            background = backgroundColor,
            onBackground = LightOnBackground,
            surface = backgroundColor,
            onSurface = LightOnSurface,
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = LightOnSurfaceVariant,
            outline = LightOutline,
            outlineVariant = LightOutlineVariant,
            surfaceContainerLowest = LightSurfaceContainerLowest,
            surfaceContainerLow = LightSurfaceContainerLow,
            surfaceContainer = LightSurfaceContainer,
            surfaceContainerHigh = LightSurfaceContainerHigh,
            surfaceContainerHighest = LightSurfaceContainerHighest,
            inverseSurface = LightInverseSurface,
            inverseOnSurface = LightInverseOnSurface,
            inversePrimary = LightInversePrimary,
            error = LightError,
            onError = LightOnError,
            errorContainer = LightErrorContainer,
            onErrorContainer = LightOnErrorContainer
        )
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalAppearance provides appearance) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PteronTypography,
            content = content
        )
    }
}

fun AccentColor.toComposeColor(): Color = hex(this.hex)
fun FolderTone.toComposeColor(): Color = hex(this.hex)
fun BackgroundTheme.toComposeColor(): Color = hex(this.backgroundHex)
