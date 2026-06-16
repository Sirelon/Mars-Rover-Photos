package com.sirelon.marsroverphotos.platform

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
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

    val dbPath = context.getDatabasePath("mars-rover-photos-database").absolutePath
    return Room.databaseBuilder<AppDataBase>(name = dbPath)
        .configureCommon()
}
