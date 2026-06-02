package com.sirelon.marsroverphotos.platform

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.sirelon.marsroverphotos.data.database.AppDataBase
import java.io.File

actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDataBase> {
    val userHome = System.getProperty("user.home")
    val appDir = File(userHome, ".marsroverphotos")
    if (!appDir.exists()) {
        appDir.mkdirs()
    }
    val dbPath = File(appDir, "mars-rover-photos-database").absolutePath

    return Room.databaseBuilder<AppDataBase>(name = dbPath)
        .setDriver(BundledSQLiteDriver())
        .fallbackToDestructiveMigration(false)
        .addMigrations(AppDataBase.migration7To8, AppDataBase.migration8To9)
}
