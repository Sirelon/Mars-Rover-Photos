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
        const val KEY_DISMISSED_UPDATE_VERSION = "dismissedUpdateVersion"
        const val KEY_NOTIFICATIONS_ENABLED = "notificationsEnabled"
        const val KEY_NOTIFICATION_OPT_IN_PENDING = "notificationOptInPending"
        const val KEY_LAUNCH_COUNT = "launchCount"
        const val KEY_FIRST_LAUNCH_AT = "firstLaunchAt"
        const val KEY_FIRST_LAUNCH_VERSION = "firstLaunchVersion"
        const val KEY_ROVER_OPEN_COUNT = "roverOpenCount"
        const val KEY_LAST_REVIEW_PROMPT_AT = "lastReviewPromptAt"
        const val KEY_LAST_REVIEW_PROMPT_VERSION = "lastReviewPromptVersion"
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

    /**
     * Set while an opt-in is waiting on the OS permission dialog, cleared once the answer is known.
     *
     * The dialog outlives the Activity — rotation recreates it, and the system can kill the process
     * outright — either of which drops the result callback after the user has already granted
     * permission. Finding this still set alongside a granted status is what tells an interrupted
     * opt-in apart from a deliberate opt-out, which otherwise look identical.
     */
    var notificationOptInPending: Boolean
        get() = preferences.getBoolean(KEY_NOTIFICATION_OPT_IN_PENDING, false)
        set(value) = preferences.setBoolean(KEY_NOTIFICATION_OPT_IN_PENDING, value)

    /**
     * The newest version whose release notes the user has acknowledged — opened or dismissed the
     * what's-new card for. Compared against the installed build to tell whether its notes are
     * still unread.
     */
    var lastSeenVersion: String?
        get() = preferences.getString(KEY_LAST_SEEN_VERSION, "").takeIf { it.isNotEmpty() }
        set(value) { preferences.setString(KEY_LAST_SEEN_VERSION, value.orEmpty()) }

    /**
     * The version the update card was last dismissed for. Kept apart from [lastSeenVersion] on
     * purpose: waving away "update to 5.4.0" says nothing about having read 5.4.0's notes, which
     * still deserve their card once 5.4.0 is actually installed.
     */
    var dismissedUpdateVersion: String?
        get() = preferences.getString(KEY_DISMISSED_UPDATE_VERSION, "").takeIf { it.isNotEmpty() }
        set(value) { preferences.setString(KEY_DISMISSED_UPDATE_VERSION, value.orEmpty()) }

    /** How many times the app has been opened, as counted by [recordLaunch]. */
    val launchCount: Int
        get() = preferences.getInt(KEY_LAUNCH_COUNT, 0)

    /** Epoch milliseconds of the first counted launch; null until [recordLaunch] has run once. */
    val firstLaunchAt: Long?
        get() = preferences.getLong(KEY_FIRST_LAUNCH_AT, 0L).takeIf { it > 0L }

    /**
     * The build that was running at the first counted launch. Together with [lastSeenVersion] it
     * separates a fresh install of a version (nothing to call "new") from an update to it.
     */
    val firstLaunchVersion: String?
        get() = preferences.getString(KEY_FIRST_LAUNCH_VERSION, "").takeIf { it.isNotEmpty() }

    /**
     * Counts an app open. Called once per launch by the app root; the first call also pins
     * [firstLaunchAt] and [firstLaunchVersion], which never move afterwards.
     */
    fun recordLaunch(versionName: String, nowMillis: Long) {
        preferences.setInt(KEY_LAUNCH_COUNT, launchCount + 1)
        if (firstLaunchAt == null) {
            preferences.setLong(KEY_FIRST_LAUNCH_AT, nowMillis)
            preferences.setString(KEY_FIRST_LAUNCH_VERSION, versionName)
        }
    }

    /** How many rovers the user has opened, across sessions, as counted by [recordRoverOpened]. */
    val roverOpenCount: Int
        get() = preferences.getInt(KEY_ROVER_OPEN_COUNT, 0)

    /** Counts a rover tap and returns the new total. */
    fun recordRoverOpened(): Int {
        val count = roverOpenCount + 1
        preferences.setInt(KEY_ROVER_OPEN_COUNT, count)
        return count
    }

    /** When the store review sheet was last requested, in epoch milliseconds; null if never. */
    var lastReviewPromptAt: Long?
        get() = preferences.getLong(KEY_LAST_REVIEW_PROMPT_AT, 0L).takeIf { it > 0L }
        set(value) { preferences.setLong(KEY_LAST_REVIEW_PROMPT_AT, value ?: 0L) }

    /** The build the store review sheet was last requested on. */
    var lastReviewPromptVersion: String?
        get() = preferences.getString(KEY_LAST_REVIEW_PROMPT_VERSION, "").takeIf { it.isNotEmpty() }
        set(value) { preferences.setString(KEY_LAST_REVIEW_PROMPT_VERSION, value.orEmpty()) }

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
