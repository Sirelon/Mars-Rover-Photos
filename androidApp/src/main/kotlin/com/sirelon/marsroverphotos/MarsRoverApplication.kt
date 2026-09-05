package com.sirelon.marsroverphotos

import android.app.Application
import androidx.work.Configuration
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.sirelon.marsroverphotos.di.initKoin
import com.sirelon.marsroverphotos.di.platformModule
import com.sirelon.marsroverphotos.domain.repositories.RoversRepository
import com.sirelon.marsroverphotos.platform.BuildInfo
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
class MarsRoverApplication : Application(), Configuration.Provider {

    private val roversRepository: RoversRepository by inject()

    /**
     * Supplies WorkManager on demand. The manifest removes its startup initializer, so WorkManager
     * is built on the first [androidx.work.WorkManager.getInstance] call — which only the widget
     * makes — instead of on every cold start. See the provider comment in AndroidManifest.xml.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.ERROR)
            .build()

    override fun onCreate() {
        super.onCreate()

        Logger.d("MarsRoverApplication") { "App starting" }

        // Initialize BuildInfo
        BuildInfo.init(
            versionName = BuildConfig.VERSION_NAME,
            isDebug = BuildConfig.DEBUG,
            packageName = BuildConfig.APPLICATION_ID
        )

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

        // Initialize Firebase Crashlytics
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.setCrashlyticsCollectionEnabled(true)
            Logger.d("MarsRoverApplication") { "Firebase Crashlytics initialized" }
        } catch (e: Exception) {
            // Firebase not configured (google-services.json missing)
            Logger.w("MarsRoverApplication") { "Firebase Crashlytics initialization failed: ${e.message}" }
        }

        // Initialize AdMob
        try {
            // Register test devices so debug ad clicks don't count as invalid traffic.
            // Test device IDs are logged by the SDK ("setTestDeviceIds(...)").
            if (BuildConfig.DEBUG) {
                MobileAds.setRequestConfiguration(
                    RequestConfiguration.Builder()
                        .setTestDeviceIds(listOf("5E9A79263E2CEF0CABB3EB5C02E071D0"))
                        .build()
                )
            }
            MobileAds.initialize(this) { status ->
                Logger.d("MarsRoverApplication") { "AdMob init status: $status" }
            }
        } catch (e: Exception) {
            Logger.w("MarsRoverApplication") { "AdMob initialization failed: ${e.message}" }
        }
    }
}
