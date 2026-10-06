package com.pteron.player.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pteron.player.data.model.SortDirection
import com.pteron.player.data.model.SortOption
import com.pteron.player.data.model.ViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

private val Context.appearanceDataStore by preferencesDataStore(name = "appearance_prefs")

data class AppearanceState(
    val theme: AppTheme = AppTheme.DEFAULT,
    val showVideoCountBadge: Boolean = true,
    val showFolderSizeBadge: Boolean = true,
    val gestureSensitivity: Float = 1.0f,
    val viewMode: ViewMode = ViewMode.GRID,
    val sortOption: SortOption = SortOption.DATE_ADDED,
    val sortDirection: SortDirection = SortDirection.DESCENDING,
    val defaultPlaybackSpeed: Float = 1.0f,
    /** Shade adjustments, -1 (lighter) .. +1 (darker); 0 keeps the theme's own shade. */
    val canvasShade: Float = 0f,
    val cardShade: Float = 0f,
    val folderShade: Float = 0f,
    val font: AppFont = AppFont.DEFAULT,
    val fontScale: Float = 1f,
    /** Videos per row in the video grid view: 2 or 3. */
    val videoGridColumns: Int = 2
)

/**
 * Every toggle exposed on the Settings screen persists here. Nothing in the
 * appearance UI is decorative -- each control reads and writes a real key.
 */
class AppearancePrefsRepository(private val context: Context) {

    private object Keys {
        val APP_THEME = stringPreferencesKey("app_theme")
        val SHOW_VIDEO_COUNT = booleanPreferencesKey("show_video_count_badge")
        val SHOW_FOLDER_SIZE = booleanPreferencesKey("show_folder_size_badge")
        val GESTURE_SENSITIVITY = floatPreferencesKey("gesture_sensitivity")
        val VIEW_MODE = stringPreferencesKey("view_mode")
        val SORT_OPTION = stringPreferencesKey("sort_option")
        val SORT_DIRECTION = stringPreferencesKey("sort_direction")
        val DEFAULT_SPEED = floatPreferencesKey("default_playback_speed")
        val LAST_LIGHT_THEME = stringPreferencesKey("last_light_theme")
        val LAST_DARK_THEME = stringPreferencesKey("last_dark_theme")
        val CANVAS_SHADE = floatPreferencesKey("canvas_shade")
        val CARD_SHADE = floatPreferencesKey("card_shade")
        val FOLDER_SHADE = floatPreferencesKey("folder_shade")
        val APP_FONT = stringPreferencesKey("app_font")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val VIDEO_GRID_COLUMNS = intPreferencesKey("video_grid_columns")
    }

    // DataStore can only be read asynchronously, which would mean a first frame in the wrong
    // theme (a light flash for dark-theme users). This tiny file remembers the last theme so it
    // can be read synchronously at startup (theme, shades and font); it is kept in sync by [theme] below.
    private val themeCache by lazy { context.getSharedPreferences("theme_cache", Context.MODE_PRIVATE) }

    /** The last applied look, available instantly. Only meant for the very first frame. */
    fun cachedThemeSettings(): ThemeSettings = ThemeSettings(
        theme = AppTheme.fromName(themeCache.getString("theme", null)),
        canvasShade = themeCache.getFloat("canvas", 0f),
        cardShade = themeCache.getFloat("card", 0f),
        folderShade = themeCache.getFloat("folder", 0f),
        font = AppFont.fromName(themeCache.getString("font", null)),
        fontScale = themeCache.getFloat("font_scale", 1f)
    )

    /** Just the look (theme, shades, font): emits only when it really changes, so the app theme isn't rebuilt for every other setting. */
    val theme: Flow<ThemeSettings> = context.appearanceDataStore.data
        .map { prefs ->
            ThemeSettings(
                theme = AppTheme.fromName(prefs[Keys.APP_THEME]),
                canvasShade = prefs[Keys.CANVAS_SHADE] ?: 0f,
                cardShade = prefs[Keys.CARD_SHADE] ?: 0f,
                folderShade = prefs[Keys.FOLDER_SHADE] ?: 0f,
                font = AppFont.fromName(prefs[Keys.APP_FONT]),
                fontScale = prefs[Keys.FONT_SCALE] ?: 1f
            )
        }
        .distinctUntilChanged()
        .onEach {
            themeCache.edit()
                .putString("theme", it.theme.name)
                .putFloat("canvas", it.canvasShade)
                .putFloat("card", it.cardShade)
                .putFloat("folder", it.folderShade)
                .putString("font", it.font.name)
                .putFloat("font_scale", it.fontScale)
                .apply()
        }

    val state: Flow<AppearanceState> = context.appearanceDataStore.data.map { prefs ->
        AppearanceState(
            theme = AppTheme.fromName(prefs[Keys.APP_THEME]),
            showVideoCountBadge = prefs[Keys.SHOW_VIDEO_COUNT] ?: true,
            showFolderSizeBadge = prefs[Keys.SHOW_FOLDER_SIZE] ?: true,
            gestureSensitivity = prefs[Keys.GESTURE_SENSITIVITY] ?: 1.0f,
            viewMode = runCatching { ViewMode.valueOf(prefs[Keys.VIEW_MODE] ?: ViewMode.GRID.name) }
                .getOrDefault(ViewMode.GRID),
            sortOption = runCatching { SortOption.valueOf(prefs[Keys.SORT_OPTION] ?: SortOption.DATE_ADDED.name) }
                .getOrDefault(SortOption.DATE_ADDED),
            sortDirection = runCatching {
                SortDirection.valueOf(prefs[Keys.SORT_DIRECTION] ?: SortDirection.DESCENDING.name)
            }.getOrDefault(SortDirection.DESCENDING),
            defaultPlaybackSpeed = prefs[Keys.DEFAULT_SPEED] ?: 1.0f,
            canvasShade = prefs[Keys.CANVAS_SHADE] ?: 0f,
            cardShade = prefs[Keys.CARD_SHADE] ?: 0f,
            folderShade = prefs[Keys.FOLDER_SHADE] ?: 0f,
            font = AppFont.fromName(prefs[Keys.APP_FONT]),
            fontScale = prefs[Keys.FONT_SCALE] ?: 1f,
            videoGridColumns = (prefs[Keys.VIDEO_GRID_COLUMNS] ?: 2).coerceIn(2, 3)
        )
    }

    suspend fun setTheme(theme: AppTheme) = edit {
        it[Keys.APP_THEME] = theme.name
        // Remembered per family so the quick dark-mode toggle can restore whichever light/dark
        // theme the person actually chose, instead of always jumping to a fixed default.
        if (theme.isDark) it[Keys.LAST_DARK_THEME] = theme.name else it[Keys.LAST_LIGHT_THEME] = theme.name
    }

    /** Flips between the last light theme and the last dark theme the person used. */
    suspend fun toggleDarkMode() {
        val prefs = context.appearanceDataStore.data.first()
        val current = AppTheme.fromName(prefs[Keys.APP_THEME])
        val lastLight = prefs[Keys.LAST_LIGHT_THEME]?.let { AppTheme.fromName(it) } ?: AppTheme.DEFAULT
        val lastDark = prefs[Keys.LAST_DARK_THEME]?.let { AppTheme.fromName(it) } ?: AppTheme.ESPRESSO
        setTheme(if (current.isDark) lastLight else lastDark)
    }
    suspend fun setCanvasShade(value: Float) = edit { it[Keys.CANVAS_SHADE] = value.coerceIn(-1f, 1f) }
    suspend fun setCardShade(value: Float) = edit { it[Keys.CARD_SHADE] = value.coerceIn(-1f, 1f) }
    suspend fun setFolderShade(value: Float) = edit { it[Keys.FOLDER_SHADE] = value.coerceIn(-1f, 1f) }
    suspend fun resetShades() = edit {
        it.remove(Keys.CANVAS_SHADE)
        it.remove(Keys.CARD_SHADE)
        it.remove(Keys.FOLDER_SHADE)
    }
    suspend fun setFont(font: AppFont) = edit { it[Keys.APP_FONT] = font.name }
    suspend fun setFontScale(scale: Float) = edit { it[Keys.FONT_SCALE] = scale.coerceIn(0.8f, 1.4f) }
    suspend fun setVideoGridColumns(columns: Int) = edit { it[Keys.VIDEO_GRID_COLUMNS] = columns.coerceIn(2, 3) }
    suspend fun setShowVideoCountBadge(value: Boolean) = edit { it[Keys.SHOW_VIDEO_COUNT] = value }
    suspend fun setShowFolderSizeBadge(value: Boolean) = edit { it[Keys.SHOW_FOLDER_SIZE] = value }
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
