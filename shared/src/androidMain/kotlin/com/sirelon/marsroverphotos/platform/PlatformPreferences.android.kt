package com.sirelon.marsroverphotos.platform

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Android implementation of PlatformPreferences using SharedPreferences.
 */
class AndroidPlatformPreferences(private val sharedPreferences: SharedPreferences) :
    PlatformPreferences {

    override fun getInt(key: String, defaultValue: Int): Int {
        return sharedPreferences.getInt(key, defaultValue)
    }

    override fun setInt(key: String, value: Int) {
        sharedPreferences.edit { putInt(key, value) }
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    override fun setBoolean(key: String, value: Boolean) {
        sharedPreferences.edit { putBoolean(key, value) }
    }

    override fun getString(key: String, defaultValue: String): String {
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    override fun setString(key: String, value: String) {
        sharedPreferences.edit { putString(key, value) }
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return sharedPreferences.getLong(key, defaultValue)
    }

    override fun setLong(key: String, value: Long) {
        sharedPreferences.edit { putLong(key, value) }
    }

    override fun remove(key: String) {
        sharedPreferences.edit { remove(key) }
    }

    override fun clear() {
        sharedPreferences.edit { clear() }
    }

    override fun contains(key: String): Boolean {
        return sharedPreferences.contains(key)
    }
}

private var androidContext: Context? = null

/**
 * Initialize Android preferences with application context.
 * Should be called once on app startup.
 */
fun initAndroidPreferences(context: Context) {
    androidContext = context.applicationContext
}

/**
 * Create Android SharedPreferences-based preferences instance.
 */
actual fun createPlatformPreferences(): PlatformPreferences {
    val context = androidContext
        ?: throw IllegalStateException("Android context not initialized. Call initAndroidPreferences() first.")

    val sharedPreferences = context.getSharedPreferences("mars-rover-photos", Context.MODE_PRIVATE)
    return AndroidPlatformPreferences(sharedPreferences)
}
