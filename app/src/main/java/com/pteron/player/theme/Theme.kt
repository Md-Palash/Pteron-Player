package com.pteron.player.theme

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import com.pteron.player.data.prefs.AppTheme
import com.pteron.player.data.prefs.ThemeSettings

/**
 * The shades of a theme, as Compose colors.
 *  - [background]: the canvas
 *  - [card]: cards, top bar, bottom bar (medium shade)
 *  - [accent]: folder icons, selected options, toggles (dark shade)
 *  - [folder]: the folder tiles in the Library (starts out equal to [card])
 */
@Immutable
data class ThemeColors(
    val background: Color,
    val card: Color,
    val accent: Color,
    val folder: Color,
    val isDark: Boolean
)

/** The resolved colors of the running theme; read it for [ThemeColors.folder] and [ThemeColors.isDark]. */
val LocalThemeColors = staticCompositionLocalOf { AppTheme.DEFAULT.colors() }

private fun parseHex(hex: String) = Color(android.graphics.Color.parseColor(hex))

/**
 * Moves a color's lightness. [shade] is -1 (lighter) .. +1 (darker); [range] is how much
 * lightness the extreme of the slider is worth. Hue and saturation are kept.
 */
private fun shaded(color: Color, shade: Float, range: Float): Color {
    if (shade == 0f) return color
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(color.toArgb(), hsl)
    hsl[2] = (hsl[2] - shade * range).coerceIn(0.02f, 0.98f)
    return Color(ColorUtils.HSLToColor(hsl))
}

/**
 * Resolves a theme to real colors. The medium (card) shade is pulled a little toward the accent
 * so cards read as separate surfaces floating on the canvas instead of being nearly identical to
 * it; the shade arguments are the person's own slider adjustments on top of that.
 */
fun AppTheme.colors(canvasShade: Float = 0f, cardShade: Float = 0f, folderShade: Float = 0f): ThemeColors {
    val accent = parseHex(accentHex)
    val card = shaded(mix(parseHex(cardHex), accent, if (isDark) 0.06f else 0.10f), cardShade, 0.18f)
    return ThemeColors(
        background = shaded(parseHex(backgroundHex), canvasShade, 0.12f),
        card = card,
        accent = accent,
        folder = shaded(card, folderShade, 0.18f),
        isDark = isDark
    )
}

fun ThemeSettings.colors(): ThemeColors = theme.colors(canvasShade, cardShade, folderShade)

/** Mixes [a] toward [b] by [t] (0 = a, 1 = b), producing a solid, opaque color. */
private fun mix(a: Color, b: Color, t: Float) = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = 1f
)

private val DarkText = Color(0xFF1A1410)

/** White or near-black, whichever is easier to read on [container]. */
fun readableOn(container: Color): Color {
    val l = container.luminance()
    val contrastWithWhite = 1.05f / (l + 0.05f)
    val contrastWithDark = (l + 0.05f) / (DarkText.luminance() + 0.05f)
    return if (contrastWithWhite >= contrastWithDark) Color.White else DarkText
}

/**
 * The accent as used on top of the always-black video overlay: a deep accent (light themes) would
 * nearly disappear there, so its lightness is lifted while keeping the same hue and saturation.
 */
fun Color.forVideoOverlay(): Color {
    if (luminance() >= 0.2f) return this
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(toArgb(), hsl)
    hsl[2] = hsl[2].coerceAtLeast(0.62f)
    return Color(ColorUtils.HSLToColor(hsl))
}

/**
 * Maps the three shades onto Material's color roles, so every screen that uses
 * `MaterialTheme.colorScheme` follows the theme without knowing about it:
 *
 *  - background                          -> light shade
 *  - surface + every surfaceContainer    -> medium shade (cards, top/bottom bars, dialogs, menus)
 *  - primary / secondaryContainer / ...  -> dark shade (folders, selected options, switches)
 */
private fun ThemeColors.toColorScheme(): ColorScheme {
    val onAccent = readableOn(accent)
    val text = if (isDark) mix(accent, Color.White, 0.88f) else mix(accent, Color.Black, 0.85f)
    val textSecondary = mix(text, background, 0.35f)
    val outline = mix(card, accent, 0.55f)
    val outlineVariant = mix(card, accent, 0.25f)
    // Slightly deeper than a card: the empty part of sliders and the "off" track of switches.
    val trackTone = mix(card, accent, 0.20f)

    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accent,
        onPrimaryContainer = onAccent,
        inversePrimary = accent,
        secondary = accent,
        onSecondary = onAccent,
        secondaryContainer = accent,
        onSecondaryContainer = onAccent,
        tertiary = accent,
        onTertiary = onAccent,
        tertiaryContainer = accent,
        onTertiaryContainer = onAccent,
        background = background,
        onBackground = text,
        surface = card,
        onSurface = text,
        surfaceVariant = card,
        onSurfaceVariant = textSecondary,
        surfaceTint = Color.Transparent,
        inverseSurface = text,
        inverseOnSurface = background,
        outline = outline,
        outlineVariant = outlineVariant,
        surfaceBright = card,
        surfaceDim = background,
        surfaceContainerLowest = card,
        surfaceContainerLow = card,
        surfaceContainer = card,
        surfaceContainerHigh = card,
        surfaceContainerHighest = trackTone,
        error = if (isDark) Color(0xFFFFB4AB) else Color(0xFFBA1A1A),
        onError = if (isDark) Color(0xFF690005) else Color.White,
        errorContainer = if (isDark) Color(0xFF93000A) else Color(0xFFFFDAD6),
        onErrorContainer = if (isDark) Color(0xFFFFDAD6) else Color(0xFF93000A)
    )
}

@Composable
fun PteronTheme(
    settings: ThemeSettings = ThemeSettings(),
    content: @Composable () -> Unit
) {
    val target = remember(settings) { settings.colors() }

    // Cross-fades the three shades themselves when a new theme is picked, instead of every
    // Material color role snapping at once -- a soft, unified transition rather than a flicker
    // across dozens of surfaces. Normal recompositions (not a theme change) settle instantly
    // since target == current already.
    val animSpec = tween<Color>(durationMillis = 280, easing = FastOutSlowInEasing)
    val animatedBackground by animateColorAsState(target.background, animSpec, label = "themeBackground")
    val animatedCard by animateColorAsState(target.card, animSpec, label = "themeCard")
    val animatedAccent by animateColorAsState(target.accent, animSpec, label = "themeAccent")
    val animatedFolder by animateColorAsState(target.folder, animSpec, label = "themeFolder")
    val colors = remember(animatedBackground, animatedCard, animatedAccent, animatedFolder, target.isDark) {
        ThemeColors(animatedBackground, animatedCard, animatedAccent, animatedFolder, target.isDark)
    }
    val colorScheme = remember(colors) { colors.toColorScheme() }
    val typography = remember(settings.font) { pteronTypography(settings.font) }

    // The theme decides light or dark, not the phone: keep the status/navigation bar icons
    // readable, and paint the window itself so nothing flashes through during transitions.
    val view = LocalView.current
    if (!view.isInEditMode) {
        LaunchedEffect(settings) {
            val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
            window.setBackgroundDrawable(ColorDrawable(target.background.toArgb()))
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !target.isDark
            controller.isAppearanceLightNavigationBars = !target.isDark
        }
    }

    CompositionLocalProvider(LocalThemeColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
