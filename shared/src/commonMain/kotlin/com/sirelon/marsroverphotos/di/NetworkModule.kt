package com.sirelon.marsroverphotos.di

import com.sirelon.marsroverphotos.data.network.RestApi
import org.koin.dsl.module

/**
 * Koin module for network dependencies.
 * Provides Ktor clients and API services.
 */
val networkModule = module {
    // REST API (singleton)
    single { RestApi() }
}
