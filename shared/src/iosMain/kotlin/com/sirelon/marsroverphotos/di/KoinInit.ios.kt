package com.sirelon.marsroverphotos.di

import com.sirelon.marsroverphotos.platform.BuildInfo
import org.koin.core.context.startKoin
import platform.Foundation.NSBundle

/**
 * Initialize Koin for iOS.
 * Called from Swift code on app launch.
 */
fun initKoinIos() {
    BuildInfo.init(
        versionName = NSBundle.mainBundle.infoDictionary
            ?.get("CFBundleShortVersionString") as? String ?: "unknown",
        isDebug = false,
        packageName = NSBundle.mainBundle.bundleIdentifier ?: "com.sirelon.marsroverphotos"
    )

    startKoin {
        modules(
            platformModule,      // iOS-specific dependencies
            databaseModule,      // Room database and DAOs
            networkModule,       // Ktor and REST API
            repositoryModule,    // Repository implementations
            viewModelModule      // ViewModels
        )
    }
}
