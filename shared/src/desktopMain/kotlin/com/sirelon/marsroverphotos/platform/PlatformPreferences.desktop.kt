package com.sirelon.marsroverphotos.platform

import java.util.prefs.Preferences

/**
 * Desktop implementation of PlatformPreferences using Java Preferences API.
 */
class DesktopPlatformPreferences(private val preferences: Preferences) : PlatformPreferences {

    override fun getInt(key: String, defaultValue: Int): Int {
        return preferences.getInt(key, defaultValue)
    }

    override fun setInt(key: String, value: Int) {
        preferences.putInt(key, value)
        preferences.flush()
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return preferences.getBoolean(key, defaultValue)
    }

    override fun setBoolean(key: String, value: Boolean) {
        preferences.putBoolean(key, value)
        preferences.flush()
    }

    override fun getString(key: String, defaultValue: String): String {
        return preferences.get(key, defaultValue)
    }

    override fun setString(key: String, value: String) {
        preferences.put(key, value)
        preferences.flush()
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return preferences.getLong(key, defaultValue)
    }

    override fun setLong(key: String, value: Long) {
        preferences.putLong(key, value)
        preferences.flush()
    }

    override fun remove(key: String) {
        preferences.remove(key)
        preferences.flush()
    }

    override fun clear() {
        preferences.clear()
        preferences.flush()
    }

    override fun contains(key: String): Boolean {
        return preferences.get(key, null) != null
    }
}

/**
 * Create Desktop Java Preferences-based preferences instance.
 */
actual fun createPlatformPreferences(): PlatformPreferences {
    val preferences = Preferences.userNodeForPackage(PlatformPreferences::class.java)
    return DesktopPlatformPreferences(preferences)
}
