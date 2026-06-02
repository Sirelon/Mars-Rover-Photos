package com.sirelon.marsroverphotos.di

import com.sirelon.marsroverphotos.data.repositories.MissionRepositoryImpl
import com.sirelon.marsroverphotos.domain.repositories.MissionRepository
import com.sirelon.marsroverphotos.domain.settings.AppSettings
import com.sirelon.marsroverphotos.platform.AppReview
import com.sirelon.marsroverphotos.platform.FirebaseAnalytics
import com.sirelon.marsroverphotos.platform.IosAppReview
import com.sirelon.marsroverphotos.platform.FirebasePhotosImpl
import com.sirelon.marsroverphotos.platform.FirebaseTracker
import com.sirelon.marsroverphotos.platform.IFirebasePhotos
import com.sirelon.marsroverphotos.platform.ImageOperations
import com.sirelon.marsroverphotos.platform.PlatformPreferences
import com.sirelon.marsroverphotos.platform.Tracker
import com.sirelon.marsroverphotos.platform.createImageOperations
import com.sirelon.marsroverphotos.platform.createPlatformPreferences
import org.koin.dsl.module

/**
 * iOS platform-specific Koin module.
 * Provides iOS implementations of platform abstractions.
 */
val platformModule = module {
    // Platform preferences (NSUserDefaults)
    single<PlatformPreferences> {
        createPlatformPreferences()
    }

    // App settings
    single { AppSettings(preferences = get()) }

    // Firebase Analytics (stub for now)
    single {
        FirebaseAnalytics()
    }

    // Firebase Photos (Firestore — GitLive KMP)
    single<IFirebasePhotos> {
        FirebasePhotosImpl()
    }

    // Image Operations (Photos framework)
    single<ImageOperations> {
        createImageOperations()
    }

    // Tracker (backed by FirebaseAnalytics — shared FirebaseTracker from commonMain)
    single<Tracker> { FirebaseTracker(get()) }

    // Mission Repository (stub)
    single<MissionRepository> {
        MissionRepositoryImpl()
    }

    // In-app review — uses SKStoreReviewController; falls back to App Store URL if unavailable
    single<AppReview> { IosAppReview() }
}
