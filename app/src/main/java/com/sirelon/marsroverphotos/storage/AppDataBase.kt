package com.sirelon.marsroverphotos.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sirelon.marsroverphotos.models.Rover

/**
 * Created on 2019-03-14 12:54 for Mars-Rover-Photos.
 */
@Database(entities = [Rover::class, MarsImage::class, FactDisplay::class], version = 9)
abstract class AppDataBase : RoomDatabase() {

    abstract fun roversDao(): RoverDao

    abstract fun imagesDao(): ImagesDao

    abstract fun factDisplayDao(): FactDisplayDao
}

object DataBaseProvider {
    lateinit var dataBase: AppDataBase
        private set

    private val migration7To8 = object : Migration(7, 8) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE images ADD COLUMN description TEXT")
            database.execSQL("ALTER TABLE images ADD COLUMN credit TEXT")
        }
    }

    private val migration8To9 = object : Migration(8, 9) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `fact_displays` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `factId` TEXT NOT NULL, `displayTimestamp` INTEGER NOT NULL, `sessionId` TEXT NOT NULL)"
            )
        }
    }

    fun init(context: Context) {
        dataBase =
            Room.databaseBuilder(context, AppDataBase::class.java, "mars-rover-photos-database")
                .fallbackToDestructiveMigration(false)
                // From version 4, I've add favorite photos into database. So, I cannot use simple destructive Migration here.
                .addMigrations(migration7To8, migration8To9)
                .build()
    }

}
