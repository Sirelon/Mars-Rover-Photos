package com.sirelon.marsroverphotos.di

import android.content.Context
import android.content.SharedPreferences
import com.sirelon.marsroverphotos.data.repositories.MissionRepositoryImpl
import com.sirelon.marsroverphotos.domain.repositories.MissionRepository
import com.sirelon.marsroverphotos.domain.settings.AppSettings
import com.sirelon.marsroverphotos.platform.AndroidPlatformPreferences
import com.sirelon.marsroverphotos.platform.FirebaseAnalytics
import com.sirelon.marsroverphotos.platform.IFirebasePhotos
import com.sirelon.marsroverphotos.platform.ImageOperations
import com.sirelon.marsroverphotos.platform.PlatformPreferences
import com.google.firebase.analytics.FirebaseAnalytics as GoogleFirebaseAnalytics
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
        FirebaseAnalytics(GoogleFirebaseAnalytics.getInstance(androidContext()))
    }

    // Firebase Photos (Firestore)
    single<IFirebasePhotos> {
        com.sirelon.marsroverphotos.platform.AndroidFirebasePhotos()
    }

    // Image Operations
    single<ImageOperations> {
        com.sirelon.marsroverphotos.platform.createImageOperations()
    }

    // Mission Repository (Firebase)
    single<MissionRepository> {
        MissionRepositoryImpl()
    }
}
