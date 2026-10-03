package com.pteron.player.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.pteron.player.R
import com.pteron.player.data.prefs.AppFont

/**
 * A variable font (one file holding every weight): the four weights the app's type scale uses are
 * each pinned to their own point on the font's weight axis. Works on API 26+, which is the app's
 * minSdk.
 */
private fun variableFamily(resId: Int): FontFamily = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { weight ->
        Font(
            resId = resId,
            weight = weight,
            variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
        )
    }
)

private val InterFamily = variableFamily(R.font.inter)
private val RobotoFamily = variableFamily(R.font.roboto)
private val OutfitFamily = variableFamily(R.font.outfit)
private val PlayfairFamily = variableFamily(R.font.playfair)
private val CinzelFamily = variableFamily(R.font.cinzel)
private val PlaywriteFamily = variableFamily(R.font.playwrite)

/** Lato is not a variable font: two static files, regular for the lighter weights, bold for the heavier ones. */
private val LatoFamily = FontFamily(
    Font(R.font.lato_regular, FontWeight.Normal),
    Font(R.font.lato_regular, FontWeight.Medium),
    Font(R.font.lato_bold, FontWeight.SemiBold),
    Font(R.font.lato_bold, FontWeight.Bold)
)

/** The Compose font family for a font choice. */
fun AppFont.fontFamily(): FontFamily = when (this) {
    AppFont.SYSTEM -> FontFamily.Default
    AppFont.INTER -> InterFamily
    AppFont.ROBOTO -> RobotoFamily
    AppFont.LATO -> LatoFamily
    AppFont.OUTFIT -> OutfitFamily
    AppFont.SERIF -> FontFamily.Serif
    AppFont.PLAYFAIR -> PlayfairFamily
    AppFont.CINZEL -> CinzelFamily
    AppFont.PLAYWRITE -> PlaywriteFamily
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
