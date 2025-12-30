package com.sirelon.marsroverphotos

import android.app.Application
import com.sirelon.marsroverphotos.di.initKoin
import com.sirelon.marsroverphotos.di.platformModule
import com.sirelon.marsroverphotos.domain.repositories.RoversRepository
import com.sirelon.marsroverphotos.platform.initAndroidDatabase
import com.sirelon.marsroverphotos.utils.Logger
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level

/**
 * Application class for Mars Rover Photos.
 * Initializes shared module components and Android-specific features.
 */
class MarsRoverApplication : Application() {

    private val roversRepository: RoversRepository by inject()

    override fun onCreate() {
        super.onCreate()

        Logger.d("MarsRoverApplication") { "App starting" }

        // Initialize Android platform contexts
        initAndroidDatabase(this)
        com.sirelon.marsroverphotos.platform.initAndroidImageOperations(this)

        // Initialize Koin DI
        initKoin(platformModule) {
            androidLogger(Level.ERROR)
            androidContext(this@MarsRoverApplication)
        }

        Logger.d("MarsRoverApplication") { "Koin initialized" }

        // Initialize repositories
        roversRepository.initialize()

        Logger.d("MarsRoverApplication") { "App initialized" }

        // TODO: Initialize Firebase Crashlytics
    }
}
