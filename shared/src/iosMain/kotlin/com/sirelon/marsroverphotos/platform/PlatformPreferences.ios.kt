package com.sirelon.marsroverphotos.platform

import platform.Foundation.NSUserDefaults

/**
 * iOS implementation of PlatformPreferences using NSUserDefaults.
 */
class IosPlatformPreferences(private val userDefaults: NSUserDefaults) : PlatformPreferences {

    override fun getInt(key: String, defaultValue: Int): Int {
        return if (userDefaults.objectForKey(key) != null) {
            userDefaults.integerForKey(key).toInt()
        } else {
            defaultValue
        }
    }

    override fun setInt(key: String, value: Int) {
        userDefaults.setInteger(value.toLong(), key)
        userDefaults.synchronize()
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return if (userDefaults.objectForKey(key) != null) {
            userDefaults.boolForKey(key)
        } else {
            defaultValue
        }
    }

    override fun setBoolean(key: String, value: Boolean) {
        userDefaults.setBool(value, key)
        userDefaults.synchronize()
    }

    override fun getString(key: String, defaultValue: String): String {
        return userDefaults.stringForKey(key) ?: defaultValue
    }

    override fun setString(key: String, value: String) {
        userDefaults.setObject(value, key)
        userDefaults.synchronize()
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return if (userDefaults.objectForKey(key) != null) {
            userDefaults.integerForKey(key)
        } else {
            defaultValue
        }
    }

    override fun setLong(key: String, value: Long) {
        userDefaults.setInteger(value, key)
        userDefaults.synchronize()
    }

    override fun remove(key: String) {
        userDefaults.removeObjectForKey(key)
        userDefaults.synchronize()
    }

    override fun clear() {
        val domain = userDefaults.dictionaryRepresentation().keys
        domain.forEach { key ->
            userDefaults.removeObjectForKey(key.toString())
        }
        userDefaults.synchronize()
    }

    override fun contains(key: String): Boolean {
        return userDefaults.objectForKey(key) != null
    }
}

/**
 * Create iOS NSUserDefaults-based preferences instance.
 */
actual fun createPlatformPreferences(): PlatformPreferences {
    return IosPlatformPreferences(NSUserDefaults.standardUserDefaults)
}
