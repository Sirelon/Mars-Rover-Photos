package com.sirelon.marsroverphotos.domain.settings

import com.sirelon.marsroverphotos.platform.PlatformPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Application settings manager.
 * Wraps platform preferences with reactive state flows.
 */
class AppSettings(
    private val preferences: PlatformPreferences
) {

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_GRID_VIEW = "gridView"
        const val KEY_SHOW_FACTS = "showFacts"
    }

    private val _showFactsFlow = MutableStateFlow(preferences.getBoolean(KEY_SHOW_FACTS, true))
    val showFactsFlow: StateFlow<Boolean> = _showFactsFlow.asStateFlow()

    private val _gridViewFlow = MutableStateFlow(preferences.getBoolean(KEY_GRID_VIEW, false))
    val gridViewFlow: StateFlow<Boolean> = _gridViewFlow.asStateFlow()

    private val _themeFlow = MutableStateFlow(Theme.fromOrdinal(preferences.getInt(KEY_THEME, Theme.SYSTEM.ordinal)))
    val themeFlow: StateFlow<Theme> = _themeFlow.asStateFlow()

    /**
     * Show or hide educational facts in photo grid.
     */
    var showFacts: Boolean
        get() = preferences.getBoolean(KEY_SHOW_FACTS, true)
        set(value) {
            preferences.setBoolean(KEY_SHOW_FACTS, value)
            _showFactsFlow.value = value
        }

    /**
     * Use grid view or list view for photos.
     */
    var gridView: Boolean
        get() = preferences.getBoolean(KEY_GRID_VIEW, false)
        set(value) {
            preferences.setBoolean(KEY_GRID_VIEW, value)
            _gridViewFlow.value = value
        }

    /**
     * App theme preference.
     */
    var theme: Theme
        get() = Theme.fromOrdinal(preferences.getInt(KEY_THEME, Theme.SYSTEM.ordinal))
        set(value) {
            preferences.setInt(KEY_THEME, value.ordinal)
            _themeFlow.value = value
        }
}

/**
 * Theme options for the app.
 */
enum class Theme {
    WHITE,  // Light theme
    DARK,   // Dark theme
    SYSTEM; // Follow system theme

    companion object {
        fun fromOrdinal(ordinal: Int): Theme {
            return entries.getOrNull(ordinal) ?: SYSTEM
        }
    }
}
