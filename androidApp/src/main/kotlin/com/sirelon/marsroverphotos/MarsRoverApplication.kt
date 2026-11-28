package com.sirelon.marsroverphotos

import android.app.Application
import androidx.work.Configuration
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.sirelon.marsroverphotos.di.initKoin
import com.sirelon.marsroverphotos.di.platformModule
import com.sirelon.marsroverphotos.di.supportModule
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
        WindowMetricsFallback.install()

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
        initKoin(listOf(platformModule, supportModule(BuildConfig.REVENUECAT_API_KEY))) {
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

        // AdMob starts lazily, from AdSlot through AdMobSdk; AdMobSdk explains why.
    }
}
