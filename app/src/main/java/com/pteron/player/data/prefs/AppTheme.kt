package com.pteron.player.data.prefs

/**
 * The ready-made themes. Every theme is three shades of one color, and each shade has one job:
 *
 *  - [backgroundHex]  the lightest shade: the screen background
 *  - [cardHex]        the medium shade: every card (settings, continue watching, video tiles,
 *                     dialogs, menus) plus the top and bottom bars
 *  - [accentHex]      the darkest shade: folder icons, selected options and toggle switches
 *
 * Dark themes follow the same three roles, just inverted: a very dark background, slightly
 * lighter cards, and a bright accent.
 */
enum class AppTheme(
    val displayName: String,
    val isDark: Boolean,
    val backgroundHex: String,
    val cardHex: String,
    val accentHex: String
) {
    TERRACOTTA("Terracotta", false, "#FAF1E8", "#EFD9C6", "#9A4420"),
    SAGE("Sage", false, "#F1F4EA", "#DCE5CB", "#3F5A24"),
    OCEAN("Ocean", false, "#EEF4F9", "#D3E3EF", "#1F5478"),
    ROSE("Rose", false, "#FAEFF0", "#F0D6D9", "#8F2A3D"),
    LAVENDER("Lavender", false, "#F4F0FA", "#E3D9F2", "#55379A"),

    ESPRESSO("Espresso", true, "#1C1512", "#2E241D", "#E8925E"),
    MIDNIGHT("Midnight", true, "#0E141C", "#1B2735", "#6FB3E8"),
    ONYX("Onyx", true, "#000000", "#1A1A1A", "#FF8F5A");

    /** Background as an ARGB int, for the window background (drawn before Compose is ready). */
    val backgroundArgb: Int get() = android.graphics.Color.parseColor(backgroundHex)

    companion object {
        val DEFAULT = TERRACOTTA

        fun fromName(name: String?): AppTheme = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
