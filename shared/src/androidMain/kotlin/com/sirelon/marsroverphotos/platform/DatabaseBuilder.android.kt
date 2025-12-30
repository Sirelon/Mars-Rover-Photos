package com.sirelon.marsroverphotos.platform

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sirelon.marsroverphotos.data.database.AppDataBase

/**
 * Android database builder using Context.
 * Requires Android context to be set via initAndroidDatabase().
 */
private var androidContext: Context? = null

fun initAndroidDatabase(context: Context) {
    androidContext = context.applicationContext
}

actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDataBase> {
    val context = androidContext
        ?: throw IllegalStateException("Android context not initialized. Call initAndroidDatabase() first.")

    return Room.databaseBuilder(
        context,
        AppDataBase::class.java,
        "mars-rover-photos-database"
    )
        .fallbackToDestructiveMigration(false)
        .addMigrations(AppDataBase.migration7To8, AppDataBase.migration8To9)
}
