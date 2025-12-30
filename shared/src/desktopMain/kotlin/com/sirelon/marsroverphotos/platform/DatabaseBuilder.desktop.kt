package com.sirelon.marsroverphotos.platform

import androidx.room.Room
import androidx.room.RoomDatabase
import com.sirelon.marsroverphotos.data.database.AppDataBase
import java.io.File

/**
 * Desktop database builder using file path.
 * Stores database in user's home directory under .marsroverphotos/
 */
actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDataBase> {
    val userHome = System.getProperty("user.home")
    val appDir = File(userHome, ".marsroverphotos")

    // Create directory if it doesn't exist
    if (!appDir.exists()) {
        appDir.mkdirs()
    }

    val dbPath = File(appDir, "mars-rover-photos-database").absolutePath

    return Room.databaseBuilder<AppDataBase>(
        name = dbPath
    )
        .addMigrations(AppDataBase.migration7To8, AppDataBase.migration8To9)
}
