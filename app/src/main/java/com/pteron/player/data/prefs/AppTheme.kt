package com.pteron.player.data.prefs

/**
 * The ready-made themes. Every theme is three shades of one color, and each shade has one job:
 *
 *  - [backgroundHex]  the lightest shade: the screen background -- mostly white/near-black with
 *                     only a subtle touch of the theme color, never a strong tint
 *  - [cardHex]        the medium shade: every card (settings, continue watching, video tiles,
 *                     dialogs, menus) plus the top and bottom bars -- a soft, light version of
 *                     the theme color, closer to pastel than to a saturated block of color
 *  - [accentHex]      the darkest shade: folder icons, selected options and toggle switches --
 *                     softened rather than a harsh, fully-saturated color
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
    TERRACOTTA("Terracotta", false, "#FDF8F3", "#F6E2D2", "#A8562E"),
    SAGE("Sage", false, "#F8FAF4", "#E4EAD6", "#52713A"),
    OCEAN("Ocean", false, "#F5F9FC", "#DEEAF4", "#2E6690"),
    ROSE("Rose", false, "#FDF6F7", "#F5DFE2", "#A6394E"),
    LAVENDER("Lavender", false, "#F9F6FC", "#EAE1F5", "#6B4AAE"),

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
