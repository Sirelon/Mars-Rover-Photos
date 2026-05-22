package com.sirelon.marsroverphotos.di

import android.content.Context
import android.content.SharedPreferences
import com.sirelon.marsroverphotos.data.repositories.MissionRepositoryImpl
import com.sirelon.marsroverphotos.domain.repositories.MissionRepository
import com.sirelon.marsroverphotos.domain.settings.AppSettings
import com.sirelon.marsroverphotos.platform.AndroidAppReview
import com.sirelon.marsroverphotos.platform.AndroidPlatformPreferences
import com.sirelon.marsroverphotos.platform.AppReview
import com.sirelon.marsroverphotos.platform.FirebaseAnalytics
import com.sirelon.marsroverphotos.platform.FirebaseTracker
import com.sirelon.marsroverphotos.platform.FirebasePhotosImpl
import com.sirelon.marsroverphotos.platform.IFirebasePhotos
import com.sirelon.marsroverphotos.platform.ImageOperations
import com.sirelon.marsroverphotos.platform.PlatformPreferences
import com.sirelon.marsroverphotos.platform.Tracker
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Android platform-specific Koin module.
 * Provides Android implementations of platform abstractions.
 */
val platformModule = module {
    // SharedPreferences
    single<SharedPreferences> {
        androidContext().getSharedPreferences("mars-rover-photos", Context.MODE_PRIVATE)
    }

    // Platform preferences
    single<PlatformPreferences> {
        AndroidPlatformPreferences(sharedPreferences = get())
    }

    // App settings
    single { AppSettings(preferences = get()) }

    // Firebase Analytics
    single {
        FirebaseAnalytics()
    }

    // Firebase Photos (Firestore — GitLive KMP)
    single<IFirebasePhotos> {
        FirebasePhotosImpl()
    }

    // Image Operations
    single<ImageOperations> {
        com.sirelon.marsroverphotos.platform.createImageOperations()
    }

    // Tracker
    single<Tracker> { FirebaseTracker(get()) }

    // Mission Repository (Firebase)
    single<MissionRepository> {
        MissionRepositoryImpl()
    }

    // In-app review (Play Core)
    single<AppReview> { AndroidAppReview(context = androidContext()) }
}
