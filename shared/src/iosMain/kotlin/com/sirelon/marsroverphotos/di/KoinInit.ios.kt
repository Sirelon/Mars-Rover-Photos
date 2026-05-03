package com.sirelon.marsroverphotos.di

import org.koin.core.context.startKoin

/**
 * Initialize Koin for iOS.
 * Called from Swift code on app launch.
 */
fun initKoinIos() {
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
