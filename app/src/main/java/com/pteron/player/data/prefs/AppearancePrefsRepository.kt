package com.pteron.player.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pteron.player.data.model.SortDirection
import com.pteron.player.data.model.SortOption
import com.pteron.player.data.model.ViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appearanceDataStore by preferencesDataStore(name = "appearance_prefs")

enum class BackgroundTheme(val displayName: String, val backgroundHex: String, val isDark: Boolean) {
    WARM_ALMOND("Warm Almond", "#F5EDE4", false),
    LIGHT_CREAM("Light Cream", "#FAF5EF", false),
    RICH_ESPRESSO("Rich Espresso", "#1C1512", true),
    DARK_WALNUT("Dark Walnut", "#2A211B", true);

    companion object {
        fun fromName(name: String?): BackgroundTheme =
            entries.firstOrNull { it.name == name } ?: WARM_ALMOND
    }
}

enum class AccentColor(val displayName: String, val hex: String) {
    TERRACOTTA_AMBER("Terracotta Amber", "#D26938"),
    OCHRE_GOLD("Ochre Gold", "#DDA15E"),
    SAGE_LEAF("Sage Leaf", "#606C38"),
    NORDIC_TEAL("Nordic Teal", "#2A6F97"),
    DEEP_CLAY("Deep Clay", "#A44A3F"),
    SANDSTONE("Sandstone", "#B08968");

    companion object {
        fun fromName(name: String?): AccentColor =
            entries.firstOrNull { it.name == name } ?: TERRACOTTA_AMBER
    }
}

enum class FolderTone(val displayName: String, val hex: String) {
    CARAMEL("Caramel", "#9C7456"),
    SADDLE_BROWN("Saddle Brown", "#7B573B"),
    DEEP_WALNUT("Deep Walnut", "#5C3D28"),
    DARK_ROAST("Dark Roast", "#3E2B1F");

    companion object {
        fun fromName(name: String?): FolderTone =
            entries.firstOrNull { it.name == name } ?: CARAMEL
    }
}

data class AppearanceState(
    val backgroundTheme: BackgroundTheme = BackgroundTheme.WARM_ALMOND,
    val accentColor: AccentColor = AccentColor.TERRACOTTA_AMBER,
    val folderTone: FolderTone = FolderTone.CARAMEL,
    val showVideoCountBadge: Boolean = true,
    val showFolderSizeBadge: Boolean = true,
    val matchControlsToAccent: Boolean = true,
    val oledPureBlackControls: Boolean = false,
    val gestureSensitivity: Float = 1.0f,
    val viewMode: ViewMode = ViewMode.GRID,
    val sortOption: SortOption = SortOption.DATE_ADDED,
    val sortDirection: SortDirection = SortDirection.DESCENDING,
    val defaultPlaybackSpeed: Float = 1.0f
)

/**
 * Every toggle exposed on the Settings screen persists here. Nothing in the
 * appearance UI is decorative -- each control reads and writes a real key.
 */
class AppearancePrefsRepository(private val context: Context) {

    private object Keys {
        val BACKGROUND_THEME = stringPreferencesKey("background_theme")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val FOLDER_TONE = stringPreferencesKey("folder_tone")
        val SHOW_VIDEO_COUNT = booleanPreferencesKey("show_video_count_badge")
        val SHOW_FOLDER_SIZE = booleanPreferencesKey("show_folder_size_badge")
        val MATCH_CONTROLS_ACCENT = booleanPreferencesKey("match_controls_to_accent")
        val OLED_CONTROLS = booleanPreferencesKey("oled_pure_black_controls")
        val GESTURE_SENSITIVITY = floatPreferencesKey("gesture_sensitivity")
        val VIEW_MODE = stringPreferencesKey("view_mode")
        val SORT_OPTION = stringPreferencesKey("sort_option")
        val SORT_DIRECTION = stringPreferencesKey("sort_direction")
        val DEFAULT_SPEED = floatPreferencesKey("default_playback_speed")
    }

    val state: Flow<AppearanceState> = context.appearanceDataStore.data.map { prefs ->
        AppearanceState(
            backgroundTheme = BackgroundTheme.fromName(prefs[Keys.BACKGROUND_THEME]),
            accentColor = AccentColor.fromName(prefs[Keys.ACCENT_COLOR]),
            folderTone = FolderTone.fromName(prefs[Keys.FOLDER_TONE]),
            showVideoCountBadge = prefs[Keys.SHOW_VIDEO_COUNT] ?: true,
            showFolderSizeBadge = prefs[Keys.SHOW_FOLDER_SIZE] ?: true,
            matchControlsToAccent = prefs[Keys.MATCH_CONTROLS_ACCENT] ?: true,
            oledPureBlackControls = prefs[Keys.OLED_CONTROLS] ?: false,
            gestureSensitivity = prefs[Keys.GESTURE_SENSITIVITY] ?: 1.0f,
            viewMode = runCatching { ViewMode.valueOf(prefs[Keys.VIEW_MODE] ?: ViewMode.GRID.name) }
                .getOrDefault(ViewMode.GRID),
            sortOption = runCatching { SortOption.valueOf(prefs[Keys.SORT_OPTION] ?: SortOption.DATE_ADDED.name) }
                .getOrDefault(SortOption.DATE_ADDED),
            sortDirection = runCatching {
                SortDirection.valueOf(prefs[Keys.SORT_DIRECTION] ?: SortDirection.DESCENDING.name)
            }.getOrDefault(SortDirection.DESCENDING),
            defaultPlaybackSpeed = prefs[Keys.DEFAULT_SPEED] ?: 1.0f
        )
    }

    suspend fun setBackgroundTheme(theme: BackgroundTheme) = edit { it[Keys.BACKGROUND_THEME] = theme.name }
    suspend fun setAccentColor(color: AccentColor) = edit { it[Keys.ACCENT_COLOR] = color.name }
    suspend fun setFolderTone(tone: FolderTone) = edit { it[Keys.FOLDER_TONE] = tone.name }
    suspend fun setShowVideoCountBadge(value: Boolean) = edit { it[Keys.SHOW_VIDEO_COUNT] = value }
    suspend fun setShowFolderSizeBadge(value: Boolean) = edit { it[Keys.SHOW_FOLDER_SIZE] = value }
    suspend fun setMatchControlsToAccent(value: Boolean) = edit { it[Keys.MATCH_CONTROLS_ACCENT] = value }
    suspend fun setOledPureBlackControls(value: Boolean) = edit { it[Keys.OLED_CONTROLS] = value }
    suspend fun setGestureSensitivity(value: Float) = edit { it[Keys.GESTURE_SENSITIVITY] = value.coerceIn(0.5f, 2.0f) }
    suspend fun setViewMode(mode: ViewMode) = edit { it[Keys.VIEW_MODE] = mode.name }
    suspend fun setSortOption(option: SortOption) = edit { it[Keys.SORT_OPTION] = option.name }
    suspend fun setSortDirection(direction: SortDirection) = edit { it[Keys.SORT_DIRECTION] = direction.name }
    suspend fun setDefaultPlaybackSpeed(speed: Float) = edit { it[Keys.DEFAULT_SPEED] = speed }

    suspend fun resetToDefaults() = edit { it.clear() }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.appearanceDataStore.edit(block)
    }
}
