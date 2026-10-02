package com.pteron.player.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.pteron.player.R
import com.pteron.player.data.prefs.AppFont

/**
 * Comfortaa, the default font. IMPORTANT: this references font files that are not bundled by
 * default -- they must already be in app/src/main/res/font/ as comfortaa_light.ttf,
 * comfortaa_regular.ttf, comfortaa_medium.ttf, comfortaa_semibold.ttf, comfortaa_bold.ttf.
 *
 * Font files stay local resources (no downloadable-fonts API) because the app requests no
 * INTERNET permission.
 */
private val ComfortaaFamily = FontFamily(
    Font(R.font.comfortaa_light, FontWeight.Light),
    Font(R.font.comfortaa_regular, FontWeight.Normal),
    Font(R.font.comfortaa_medium, FontWeight.Medium),
    Font(R.font.comfortaa_semibold, FontWeight.SemiBold),
    Font(R.font.comfortaa_bold, FontWeight.Bold)
)

/** The Compose font family for a font choice. The non-Comfortaa ones are built into Android. */
fun AppFont.fontFamily(): FontFamily = when (this) {
    AppFont.COMFORTAA -> ComfortaaFamily
    AppFont.SYSTEM -> FontFamily.Default
    AppFont.SERIF -> FontFamily.Serif
    AppFont.MONOSPACE -> FontFamily.Monospace
    AppFont.CURSIVE -> FontFamily.Cursive
}

private val defaultType = Typography()

/**
 * One type scale for the whole app, so every role has a fixed size and weight:
 *  - headlineSmall  screen titles ("Settings", "All Videos")
 *  - titleLarge     app name in the Library top bar
 *  - titleMedium    section titles ("Continue Watching", "Folders"), sheet titles
 *  - titleSmall     card titles (folder name, video name, setting name)
 *  - bodyMedium / bodySmall   descriptions and supporting lines
 *  - labelLarge / labelMedium chips, buttons, counts
 *  - labelSmall     tiny badges and the small-caps line above a screen title
 */
fun pteronTypography(font: AppFont): Typography {
    val family = font.fontFamily()
    fun TextStyle.styled(
        weight: FontWeight? = null,
        size: Int? = null,
        lineHeight: Int? = null,
        letterSpacing: Float? = null
    ) = copy(
        fontFamily = family,
        fontWeight = weight ?: fontWeight,
        fontSize = size?.sp ?: fontSize,
        lineHeight = lineHeight?.sp ?: this.lineHeight,
        letterSpacing = letterSpacing?.sp ?: this.letterSpacing
    )
    return Typography(
        displayLarge = defaultType.displayLarge.styled(),
        displayMedium = defaultType.displayMedium.styled(),
        displaySmall = defaultType.displaySmall.styled(),
        headlineLarge = defaultType.headlineLarge.styled(FontWeight.SemiBold, 30, 36),
        headlineMedium = defaultType.headlineMedium.styled(FontWeight.SemiBold, 26, 32),
        headlineSmall = defaultType.headlineSmall.styled(FontWeight.SemiBold, 22, 28, 0f),
        titleLarge = defaultType.titleLarge.styled(FontWeight.SemiBold, 20, 26, 0f),
        titleMedium = defaultType.titleMedium.styled(FontWeight.SemiBold, 16, 22, 0.1f),
        titleSmall = defaultType.titleSmall.styled(FontWeight.Medium, 14, 20, 0.1f),
        bodyLarge = defaultType.bodyLarge.styled(size = 16, lineHeight = 24),
        bodyMedium = defaultType.bodyMedium.styled(size = 14, lineHeight = 20),
        bodySmall = defaultType.bodySmall.styled(size = 12, lineHeight = 16),
        labelLarge = defaultType.labelLarge.styled(FontWeight.Medium, 13, 18),
        labelMedium = defaultType.labelMedium.styled(FontWeight.Medium, 12, 16),
        labelSmall = defaultType.labelSmall.styled(size = 10, lineHeight = 14, letterSpacing = 0.3f)
    )
}
