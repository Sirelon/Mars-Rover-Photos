package com.sirelon.marsroverphotos.platform

import kotlinx.browser.localStorage

/**
 * Web implementation of PlatformPreferences using localStorage.
 */
class WebPlatformPreferences : PlatformPreferences {

    override fun getInt(key: String, defaultValue: Int): Int {
        val value = localStorage.getItem(key)
        return value?.toIntOrNull() ?: defaultValue
    }

    override fun setInt(key: String, value: Int) {
        localStorage.setItem(key, value.toString())
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        val value = localStorage.getItem(key)
        return value?.toBooleanStrictOrNull() ?: defaultValue
    }

    override fun setBoolean(key: String, value: Boolean) {
        localStorage.setItem(key, value.toString())
    }

    override fun getString(key: String, defaultValue: String): String {
        return localStorage.getItem(key) ?: defaultValue
    }

    override fun setString(key: String, value: String) {
        localStorage.setItem(key, value)
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        val value = localStorage.getItem(key)
        return value?.toLongOrNull() ?: defaultValue
    }

    override fun setLong(key: String, value: Long) {
        localStorage.setItem(key, value.toString())
    }

    override fun remove(key: String) {
        localStorage.removeItem(key)
    }

    override fun clear() {
        localStorage.clear()
    }

    override fun contains(key: String): Boolean {
        return localStorage.getItem(key) != null
    }
}

/**
 * Create Web localStorage-based preferences instance.
 */
actual fun createPlatformPreferences(): PlatformPreferences {
    return WebPlatformPreferences()
}
