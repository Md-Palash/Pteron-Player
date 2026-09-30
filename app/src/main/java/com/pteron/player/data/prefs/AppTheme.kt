package com.pteron.player.data.prefs

/**
 * The ready-made themes. Every theme is three shades of one color, and each shade has one job:
 *
 *  - [backgroundHex]  the lightest shade: the screen background, kept as close to plain
 *                     white/near-black as possible with only the faintest touch of color
 *  - [cardHex]        the medium shade: every card (settings, continue watching, video tiles,
 *                     dialogs, menus) plus the top and bottom bars -- just a touch darker than
 *                     the background, not a visibly different block of color
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
    TERRACOTTA("Terracotta", false, "#FCF9F8", "#F8F2EF", "#A8562E"),
    SAGE("Sage", false, "#F9FAF8", "#F2F4F0", "#52713A"),
    OCEAN("Ocean", false, "#F8FAFB", "#EFF4F7", "#2E6690"),
    ROSE("Rose", false, "#FCF8F9", "#F8F0F2", "#A6394E"),
    LAVENDER("Lavender", false, "#FAF9FC", "#F4F1F9", "#6B4AAE"),
    SUNSET("Sunset", false, "#FEFAF8", "#FCF4EF", "#D9702A"),
    MINT("Mint", false, "#F8FBFA", "#EFF7F4", "#2F8F6E"),
    SKY("Sky", false, "#F8FBFD", "#F1F5FA", "#3E7FB8"),
    BLUSH("Blush", false, "#FDF9FA", "#FAF3F5", "#C15A7A"),

    ESPRESSO("Espresso", true, "#19120D", "#2A1D14", "#E8925E"),
    MIDNIGHT("Midnight", true, "#131314", "#1B2126", "#6FB3E8"),
    ONYX("Onyx", true, "#000000", "#261811", "#FF8F5A"),
    SLATE("Slate", true, "#141212", "#1C1D1F", "#7C93B3"),
    FOREST("Forest", true, "#13130F", "#1B1F18", "#6FA87C");

    /** Background as an ARGB int, for the window background (drawn before Compose is ready). */
    val backgroundArgb: Int get() = android.graphics.Color.parseColor(backgroundHex)

    companion object {
        val DEFAULT = TERRACOTTA

        fun fromName(name: String?): AppTheme = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
