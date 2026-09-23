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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import com.pteron.player.data.prefs.AppTheme

/**
 * The three shades of a theme, as Compose colors.
 *  - [background]: screen background (light shade)
 *  - [card]: cards, top bar, bottom bar (medium shade)
 *  - [accent]: folder icons, selected options, toggles (dark shade)
 */
@Immutable
data class ThemeColors(
    val background: Color,
    val card: Color,
    val accent: Color,
    val isDark: Boolean
)

private fun parseHex(hex: String) = Color(android.graphics.Color.parseColor(hex))

fun AppTheme.colors(): ThemeColors =
    ThemeColors(parseHex(backgroundHex), parseHex(cardHex), parseHex(accentHex), isDark)

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
    theme: AppTheme = AppTheme.DEFAULT,
    content: @Composable () -> Unit
) {
    val target = remember(theme) { theme.colors() }

    // Cross-fades the three shades themselves when a new theme is picked, instead of every
    // Material color role snapping at once -- a soft, unified transition rather than a flicker
    // across dozens of surfaces. Normal recompositions (not a theme change) settle instantly
    // since target == current already.
    val animSpec = tween<Color>(durationMillis = 280, easing = FastOutSlowInEasing)
    val animatedBackground by animateColorAsState(target.background, animSpec, label = "themeBackground")
    val animatedCard by animateColorAsState(target.card, animSpec, label = "themeCard")
    val animatedAccent by animateColorAsState(target.accent, animSpec, label = "themeAccent")
    val colors = remember(animatedBackground, animatedCard, animatedAccent, target.isDark) {
        ThemeColors(animatedBackground, animatedCard, animatedAccent, target.isDark)
    }
    val colorScheme = remember(colors) { colors.toColorScheme() }

    // The theme decides light or dark, not the phone: keep the status/navigation bar icons
    // readable, and paint the window itself so nothing flashes through during transitions.
    val view = LocalView.current
    if (!view.isInEditMode) {
        LaunchedEffect(theme) {
            val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
            window.setBackgroundDrawable(ColorDrawable(target.background.toArgb()))
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !target.isDark
            controller.isAppearanceLightNavigationBars = !target.isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PteronTypography,
        content = content
    )
}
