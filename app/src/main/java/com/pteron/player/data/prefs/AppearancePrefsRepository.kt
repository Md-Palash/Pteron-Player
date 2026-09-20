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
import kotlinx.coroutines.flow.distinctUntilChanged
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
    val defaultPlaybackSpeed: Float = 1.0f
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
    }

    // DataStore can only be read asynchronously, which would mean a first frame in the wrong
    // theme (a light flash for dark-theme users). This tiny file remembers the last theme so it
    // can be read synchronously at startup; it is kept in sync by [theme] below.
    private val themeCache by lazy { context.getSharedPreferences("theme_cache", Context.MODE_PRIVATE) }

    /** The last applied theme, available instantly. Only meant for the very first frame. */
    fun cachedTheme(): AppTheme = AppTheme.fromName(themeCache.getString("theme", null))

    /** Just the theme: emits only when it really changes, so the app theme isn't rebuilt for every other setting. */
    val theme: Flow<AppTheme> = context.appearanceDataStore.data
        .map { prefs -> AppTheme.fromName(prefs[Keys.APP_THEME]) }
        .distinctUntilChanged()
        .onEach { themeCache.edit().putString("theme", it.name).apply() }

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
            defaultPlaybackSpeed = prefs[Keys.DEFAULT_SPEED] ?: 1.0f
        )
    }

    suspend fun setTheme(theme: AppTheme) = edit { it[Keys.APP_THEME] = theme.name }
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
