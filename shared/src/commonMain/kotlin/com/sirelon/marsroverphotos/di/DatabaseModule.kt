package com.sirelon.marsroverphotos.di

import com.sirelon.marsroverphotos.data.database.AppDataBase
import com.sirelon.marsroverphotos.platform.getDatabaseBuilder
import org.koin.dsl.module

/**
 * Koin module for database dependencies.
 * Provides Room database and DAOs.
 */
val databaseModule = module {
    // Database instance (singleton)
    single<AppDataBase> {
        getDatabaseBuilder().build()
    }

    // DAOs
    single { get<AppDataBase>().roversDao() }
    single { get<AppDataBase>().imagesDao() }
    single { get<AppDataBase>().factDisplayDao() }
}
