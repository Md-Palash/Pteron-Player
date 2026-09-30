package com.pteron.player.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.pteron.player.R

/**
 * Comfortaa, used for every piece of text in the app.
 *
 * IMPORTANT: this references font files that are not bundled by default -- add them yourself
 * before this will compile:
 *   1. Download the static weights from https://fonts.google.com/specimen/Comfortaa (the "Get
 *      font" > "Download all" button includes Comfortaa-Light/Regular/Medium/SemiBold/Bold.ttf).
 *   2. Rename them to lowercase snake_case and copy them into app/src/main/res/font/:
 *        comfortaa_light.ttf, comfortaa_regular.ttf, comfortaa_medium.ttf,
 *        comfortaa_semibold.ttf, comfortaa_bold.ttf
 *   3. That's it -- R.font.comfortaa_* will resolve once the files are in place.
 *
 * Font files are kept as local resources rather than Google's downloadable-fonts API on purpose:
 * this app requests no INTERNET permission and is fully offline (see AndroidManifest.xml), and
 * the downloadable-fonts API would need one just to fetch the typeface on first use.
 */
private val ComfortaaFamily = FontFamily(
    Font(R.font.comfortaa_light, FontWeight.Light),
    Font(R.font.comfortaa_regular, FontWeight.Normal),
    Font(R.font.comfortaa_medium, FontWeight.Medium),
    Font(R.font.comfortaa_semibold, FontWeight.SemiBold),
    Font(R.font.comfortaa_bold, FontWeight.Bold)
)

private val defaultType = Typography()

val PteronTypography = Typography(
    displayLarge = defaultType.displayLarge.copy(fontFamily = ComfortaaFamily),
    displayMedium = defaultType.displayMedium.copy(fontFamily = ComfortaaFamily),
    displaySmall = defaultType.displaySmall.copy(fontFamily = ComfortaaFamily),
    headlineLarge = defaultType.headlineLarge.copy(fontFamily = ComfortaaFamily, fontWeight = FontWeight.SemiBold),
    headlineMedium = defaultType.headlineMedium.copy(fontFamily = ComfortaaFamily, fontWeight = FontWeight.SemiBold),
    headlineSmall = defaultType.headlineSmall.copy(fontFamily = ComfortaaFamily, fontWeight = FontWeight.SemiBold),
    titleLarge = defaultType.titleLarge.copy(fontFamily = ComfortaaFamily, fontWeight = FontWeight.SemiBold),
    titleMedium = defaultType.titleMedium.copy(fontFamily = ComfortaaFamily, fontWeight = FontWeight.Medium),
    titleSmall = defaultType.titleSmall.copy(fontFamily = ComfortaaFamily, fontWeight = FontWeight.Medium),
    bodyLarge = defaultType.bodyLarge.copy(fontFamily = ComfortaaFamily),
    bodyMedium = defaultType.bodyMedium.copy(fontFamily = ComfortaaFamily),
    bodySmall = defaultType.bodySmall.copy(fontFamily = ComfortaaFamily),
    labelLarge = defaultType.labelLarge.copy(fontFamily = ComfortaaFamily, fontWeight = FontWeight.Medium),
    labelMedium = defaultType.labelMedium.copy(fontFamily = ComfortaaFamily, fontWeight = FontWeight.Medium),
    labelSmall = defaultType.labelSmall.copy(fontFamily = ComfortaaFamily)
)
