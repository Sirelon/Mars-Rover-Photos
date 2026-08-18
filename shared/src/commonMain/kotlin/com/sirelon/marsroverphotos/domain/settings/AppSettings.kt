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
        const val KEY_SHOW_CAMERA_NAME = "showCameraName"
        const val KEY_LAST_SEEN_VERSION = "lastSeenVersion"
        const val KEY_NOTIFICATIONS_ENABLED = "notificationsEnabled"
    }

    private val _notificationsEnabledFlow =
        MutableStateFlow(preferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, false))
    val notificationsEnabledFlow: StateFlow<Boolean> = _notificationsEnabledFlow.asStateFlow()

    private val _showFactsFlow = MutableStateFlow(preferences.getBoolean(KEY_SHOW_FACTS, true))
    val showFactsFlow: StateFlow<Boolean> = _showFactsFlow.asStateFlow()

    private val _showCameraNameFlow = MutableStateFlow(preferences.getBoolean(KEY_SHOW_CAMERA_NAME, true))
    val showCameraNameFlow: StateFlow<Boolean> = _showCameraNameFlow.asStateFlow()

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
     * Show or hide the camera name on photo cards.
     */
    var showCameraName: Boolean
        get() = preferences.getBoolean(KEY_SHOW_CAMERA_NAME, true)
        set(value) {
            preferences.setBoolean(KEY_SHOW_CAMERA_NAME, value)
            _showCameraNameFlow.value = value
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
     * Whether the user has opted in to push notifications.
     *
     * Tracks the user's *intent*, which is not the same as OS authorization — permission can be
     * revoked in system settings without the app being told. Read it together with
     * [com.sirelon.marsroverphotos.platform.PushNotifications.permissionStatus] rather than alone.
     */
    var notificationsEnabled: Boolean
        get() = preferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, false)
        set(value) {
            preferences.setBoolean(KEY_NOTIFICATIONS_ENABLED, value)
            _notificationsEnabledFlow.value = value
        }

    var lastSeenVersion: String?
        get() = preferences.getString(KEY_LAST_SEEN_VERSION, "").takeIf { it.isNotEmpty() }
        set(value) { preferences.setString(KEY_LAST_SEEN_VERSION, value.orEmpty()) }

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
