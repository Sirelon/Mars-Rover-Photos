package com.sirelon.marsroverphotos.storage

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sirelon.marsroverphotos.models.Rover

/**
 * Created on 2019-03-14 12:54 for Mars-Rover-Photos.
 */
@Database(
    entities = [Rover::class, MarsImage::class, FactDisplay::class],
    version = 8,
    autoMigrations = [
        AutoMigration(from = 7, to = 8)
    ]
)
abstract class AppDataBase : RoomDatabase() {

    abstract fun roversDao(): RoverDao

    abstract fun imagesDao(): ImagesDao

    abstract fun factDisplayDao(): FactDisplayDao
}

object DataBaseProvider {
    lateinit var dataBase: AppDataBase
        private set

    fun init(context: Context) {
        dataBase =
            Room.databaseBuilder(context, AppDataBase::class.java, "mars-rover-photos-database")
                .fallbackToDestructiveMigration(false)
                // From version 4, I've add favorite photos into database. So, I cannot use simple destructive Migration here.
                .build()
    }

}
