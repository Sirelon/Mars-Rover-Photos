package com.sirelon.marsroverphotos.platform

/**
 * Platform-agnostic key-value storage interface.
 * Provides persistent storage for app preferences across all platforms.
 *
 * Implementations:
 * - Android: SharedPreferences
 * - iOS: NSUserDefaults
 * - Desktop: Java Preferences API
 * - Web: localStorage
 */
interface PlatformPreferences {
    /**
     * Get an integer value.
     * @param key Preference key
     * @param defaultValue Default value if key doesn't exist
     * @return Stored value or default
     */
    fun getInt(key: String, defaultValue: Int): Int

    /**
     * Set an integer value.
     * @param key Preference key
     * @param value Value to store
     */
    fun setInt(key: String, value: Int)

    /**
     * Get a boolean value.
     * @param key Preference key
     * @param defaultValue Default value if key doesn't exist
     * @return Stored value or default
     */
    fun getBoolean(key: String, defaultValue: Boolean): Boolean

    /**
     * Set a boolean value.
     * @param key Preference key
     * @param value Value to store
     */
    fun setBoolean(key: String, value: Boolean)

    /**
     * Get a string value.
     * @param key Preference key
     * @param defaultValue Default value if key doesn't exist
     * @return Stored value or default
     */
    fun getString(key: String, defaultValue: String): String

    /**
     * Set a string value.
     * @param key Preference key
     * @param value Value to store
     */
    fun setString(key: String, value: String)

    /**
     * Get a long value.
     * @param key Preference key
     * @param defaultValue Default value if key doesn't exist
     * @return Stored value or default
     */
    fun getLong(key: String, defaultValue: Long): Long

    /**
     * Set a long value.
     * @param key Preference key
     * @param value Value to store
     */
    fun setLong(key: String, value: Long)

    /**
     * Remove a preference.
     * @param key Preference key to remove
     */
    fun remove(key: String)

    /**
     * Clear all preferences.
     */
    fun clear()

    /**
     * Check if a key exists.
     * @param key Preference key
     * @return True if key exists, false otherwise
     */
    fun contains(key: String): Boolean
}

/**
 * Factory function to create platform-specific preferences instance.
 */
expect fun createPlatformPreferences(): PlatformPreferences
