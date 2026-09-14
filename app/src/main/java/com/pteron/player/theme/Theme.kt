package com.pteron.player.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.pteron.player.data.prefs.AccentColor
import com.pteron.player.data.prefs.AppearanceState
import com.pteron.player.data.prefs.BackgroundTheme
import com.pteron.player.data.prefs.FolderTone
import com.pteron.player.data.prefs.ThemeMode

/** Exposes the raw appearance selections (accent hex, folder tone) to leaf composables
 *  that need them directly, e.g. folder cards and the player's scrub bar. */
val LocalAppearance = staticCompositionLocalOf { AppearanceState() }

private fun hex(hex: String) = Color(android.graphics.Color.parseColor(hex))

/** Mixes [color] toward black ([amount] > 0) or white ([amount] < 0), producing a
 *  solid, opaque tone -- used instead of alpha compositing so container colors stay
 *  crisp regardless of what's behind them. */
private fun shade(color: Color, amount: Float): Color {
    val target = if (amount >= 0f) Color.Black else Color.White
    val fraction = amount.coerceIn(-1f, 1f).let { if (it < 0) -it else it }
    return lerp(color, target, fraction)
}

private fun lerp(a: Color, b: Color, t: Float) = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = 1f
)

/** White text on dark/saturated containers, near-black on light/pale ones. */
private fun onColorFor(container: Color): Color =
    if (container.luminance() > 0.5f) Color(0xFF1E1B16) else Color.White

/**
 * Resolves the Light/Dark/System preference against the person's saved
 * palette choice. If System mode disagrees with the saved palette's
 * light/dark family (e.g. the phone just switched to dark mode but "Light
 * Cream" was selected), falls back to a sensible default in the requested
 * family rather than showing the wrong contrast.
 */
fun resolveEffectiveBackgroundTheme(mode: ThemeMode, selected: BackgroundTheme, systemIsDark: Boolean): BackgroundTheme {
    val wantDark = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemIsDark
    }
    return if (selected.isDark == wantDark) {
        selected
    } else if (wantDark) {
        BackgroundTheme.WARM_WALNUT
    } else {
        BackgroundTheme.WARM_ALMOND
    }
}

@Composable
fun PteronTheme(
    appearance: AppearanceState = AppearanceState(),
    content: @Composable () -> Unit
) {
    val systemIsDark = androidx.compose.foundation.isSystemInDarkTheme()
    val resolvedTheme = resolveEffectiveBackgroundTheme(appearance.themeMode, appearance.backgroundTheme, systemIsDark)

    val accent = hex(appearance.accentColor.hex)
    val isDark = resolvedTheme.isDark
    val backgroundColor = hex(resolvedTheme.backgroundHex)

    // Solid, opaque container tones derived from the chosen accent -- no alpha
    // compositing, so these look identical regardless of what's behind them.
    val primaryContainer = shade(accent, if (isDark) -0.35f else 0.22f)
    val onPrimaryContainerColor = onColorFor(primaryContainer)

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = accent,
            onPrimary = onColorFor(accent),
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainerColor,
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
            surfaceTint = accent,
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
            inversePrimary = accent,
            error = LightError,
            onError = LightOnError,
            errorContainer = LightErrorContainer,
            onErrorContainer = LightOnErrorContainer
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = onColorFor(accent),
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainerColor,
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
            surfaceTint = accent,
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
            inversePrimary = accent,
            error = LightError,
            onError = LightOnError,
            errorContainer = LightErrorContainer,
            onErrorContainer = LightOnErrorContainer
        )
    }

    CompositionLocalProvider(LocalAppearance provides appearance) {
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
