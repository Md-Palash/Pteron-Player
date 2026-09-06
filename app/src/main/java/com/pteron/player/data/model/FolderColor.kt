package com.pteron.player.data.model

import androidx.compose.ui.graphics.Color

/**
 * A curated, intentionally small palette of folder colors. Each entry
 * carries a light-theme and a dark-theme swatch so a folder stays
 * legible and calm against either background, without the user ever
 * touching a hex code.
 */
enum class FolderColor(
    val label: String,
    val light: Color,
    val dark: Color
) {
    SAGE(label = "Sage", light = Color(0xFF7E9A83), dark = Color(0xFF8FAE93)),
    CLAY(label = "Clay", light = Color(0xFFC17A5C), dark = Color(0xFFCB8A6E)),
    DUSK_BLUE(label = "Dusk Blue", light = Color(0xFF6C89A6), dark = Color(0xFF7C9BB8)),
    MUTED_GOLD(label = "Muted Gold", light = Color(0xFFB99A55), dark = Color(0xFFC7AC6D)),
    DRIED_ROSE(label = "Dried Rose", light = Color(0xFFB2777D), dark = Color(0xFFC08A90)),
    MOSS(label = "Moss", light = Color(0xFF6F8C5A), dark = Color(0xFF80A16A)),
    STONE(label = "Stone", light = Color(0xFF8C8478), dark = Color(0xFF9B948A)),
    TEAL_MIST(label = "Teal Mist", light = Color(0xFF5E9490), dark = Color(0xFF71A8A4));

    companion object {
        val Default = SAGE
    }
}
