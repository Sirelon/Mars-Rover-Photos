package com.sirelon.marsroverphotos.platform

import androidx.room.Room
import androidx.room.RoomDatabase
import com.sirelon.marsroverphotos.data.database.AppDataBase
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/**
 * iOS database builder using file path.
 * Stores database in the app's Documents directory.
 */
actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDataBase> {
    val documentsDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )?.path ?: error("Could not get documents directory")

    val dbPath = "$documentsDirectory/mars-rover-photos-database"

    return Room.databaseBuilder<AppDataBase>(
        name = dbPath
    )
        .addMigrations(AppDataBase.migration7To8, AppDataBase.migration8To9)
}
