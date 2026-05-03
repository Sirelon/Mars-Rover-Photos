package com.sirelon.marsroverphotos.platform

import androidx.room.RoomDatabase
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
