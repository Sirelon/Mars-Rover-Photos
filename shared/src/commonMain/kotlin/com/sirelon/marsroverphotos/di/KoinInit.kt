package com.sirelon.marsroverphotos.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

/**
 * Initialize Koin dependency injection.
 * Call this from each platform's application entry point.
 *
 * @param platformModules Platform-specific modules (Android, iOS, Desktop, Web): the platform
 *   module plus the store-billing / feature-flag module for that target.
 * @param appDeclaration Optional Koin configuration block
 */
fun initKoin(
    platformModules: List<Module>,
    appDeclaration: KoinAppDeclaration = {}
): KoinApplication {
    return startKoin {
        appDeclaration()
        modules(
            platformModules +    // Platform-specific dependencies (must be first)
                commonModules
        )
    }
}

/**
 * Common modules that are shared across all platforms.
 */
val commonModules = listOf(
    databaseModule,      // Room database and DAOs
    networkModule,       // Ktor and REST API
    repositoryModule,    // Repository implementations
    viewModelModule,     // ViewModels
    navigationModule,    // Navigation 3 entries
)
