package com.sirelon.marsroverphotos.presentation.di

import com.sirelon.marsroverphotos.domain.settings.AppSettings
import com.sirelon.marsroverphotos.platform.PlatformPreferences

/**
 * Dependency injection module for the app.
 * Simple factory functions for now, can be replaced with Koin later.
 */
object AppModule {

    private var appSettings: AppSettings? = null

    /**
     * Initialize the app module with platform preferences.
     */
    fun initialize(preferences: PlatformPreferences) {
        if (appSettings == null) {
            appSettings = AppSettings(preferences)
        }
    }

    /**
     * Get the app settings instance.
     */
    fun getAppSettings(): AppSettings {
        return appSettings ?: throw IllegalStateException("AppModule not initialized")
    }
}
