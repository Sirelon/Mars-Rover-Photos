package com.sirelon.marsroverphotos.di

import com.sirelon.marsroverphotos.platform.BuildInfo
import org.koin.core.context.startKoin

/**
 * Initialize Koin for Desktop (JVM).
 * Called from the desktop app's main function.
 */
fun initKoinDesktop() {
    BuildInfo.init(
        versionName = System.getProperty("app.version") ?: "unknown",
        isDebug = System.getProperty("app.debug") == "true",
        packageName = "com.sirelon.marsroverphotos"
    )

    startKoin {
        modules(
            platformModule,      // Desktop-specific dependencies
            databaseModule,      // Room database and DAOs
            networkModule,       // Ktor and REST API
            repositoryModule,    // Repository implementations
            viewModelModule      // ViewModels
        )
    }
}
