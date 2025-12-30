package com.sirelon.marsroverphotos.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

/**
 * Initialize Koin dependency injection.
 * Call this from each platform's application entry point.
 *
 * @param platformModule Platform-specific module (Android, iOS, Desktop, Web)
 * @param appDeclaration Optional Koin configuration block
 */
fun initKoin(
    platformModule: Module,
    appDeclaration: KoinAppDeclaration = {}
): KoinApplication {
    return startKoin {
        appDeclaration()
        modules(
            platformModule,      // Platform-specific dependencies (must be first)
            databaseModule,      // Room database and DAOs
            networkModule,       // Ktor and REST API
            repositoryModule,    // Repository implementations
            viewModelModule      // ViewModels
        )
    }
}

/**
 * Common modules that are shared across all platforms.
 */
val commonModules = listOf(
    databaseModule,
    networkModule,
    repositoryModule,
    viewModelModule
)
