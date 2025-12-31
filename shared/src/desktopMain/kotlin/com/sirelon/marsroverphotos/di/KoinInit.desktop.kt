package com.sirelon.marsroverphotos.di

import org.koin.core.context.startKoin

/**
 * Initialize Koin for Desktop (JVM).
 * Called from the desktop app's main function.
 */
fun initKoinDesktop() {
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
