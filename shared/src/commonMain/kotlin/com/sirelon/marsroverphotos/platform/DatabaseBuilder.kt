package com.sirelon.marsroverphotos.platform

import androidx.room3.RoomDatabase
import com.sirelon.marsroverphotos.data.database.AppDataBase

/**
 * Platform-specific database builder.
 * Each platform provides its own implementation:
 * - Android: Context-based Room builder
 * - iOS: File path-based Room builder
 * - Desktop: File path-based Room builder
 * - Web: In-memory or IndexedDB-based builder
 */
expect fun getDatabaseBuilder(): RoomDatabase.Builder<AppDataBase>

/**
 * Shared database configuration: migrations and fallback policy.
 * Called by each platform's [getDatabaseBuilder] so migration registrations live in one place.
 */
fun RoomDatabase.Builder<AppDataBase>.configureCommon(): RoomDatabase.Builder<AppDataBase> = this
    .fallbackToDestructiveMigration(false)
    .addMigrations(
        AppDataBase.migration7To8,
        AppDataBase.migration8To9,
        AppDataBase.migration9To10,
    )
