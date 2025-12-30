package com.sirelon.marsroverphotos.di

import com.sirelon.marsroverphotos.data.repositories.FactsRepositoryImpl
import com.sirelon.marsroverphotos.data.repositories.ImagesRepositoryImpl
import com.sirelon.marsroverphotos.data.repositories.PhotosRepositoryImpl
import com.sirelon.marsroverphotos.data.repositories.RoversRepositoryImpl
import com.sirelon.marsroverphotos.domain.repositories.FactsRepository
import com.sirelon.marsroverphotos.domain.repositories.ImagesRepository
import com.sirelon.marsroverphotos.domain.repositories.PhotosRepository
import com.sirelon.marsroverphotos.domain.repositories.RoversRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

/**
 * Koin module for repository dependencies.
 * Provides repository implementations with their dependencies.
 */
val repositoryModule = module {
    // Application-wide CoroutineScope for background tasks
    single<CoroutineScope> {
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    // Repositories
    single<PhotosRepository> {
        PhotosRepositoryImpl(api = get())
    }

    single<RoversRepository> {
        RoversRepositoryImpl(
            roverDao = get(),
            api = get(),
            applicationScope = get()
        )
    }

    single<ImagesRepository> {
        ImagesRepositoryImpl(
            imagesDao = get(),
            firebasePhotos = get()
        )
    }

    single<FactsRepository> {
        FactsRepositoryImpl(
            firebasePhotos = get(),
            factDisplayDao = get()
        )
    }

    // MissionRepository is expect/actual, provided by platform module
}
