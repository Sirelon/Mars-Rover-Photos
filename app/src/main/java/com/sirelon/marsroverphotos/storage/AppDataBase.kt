package com.sirelon.marsroverphotos.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sirelon.marsroverphotos.models.Rover

/**
 * Created on 2019-03-14 12:54 for Mars-Rover-Photos.
 */
@Database(entities = [Rover::class], version = 1)
abstract class AppDataBase : RoomDatabase() {

    abstract fun roversDao(): RoverDao

}

object DataBaseProvider {
    lateinit var dataBase: AppDataBase
        private set

    fun init(context: Context) {
        dataBase =
            Room.databaseBuilder(context, AppDataBase::class.java, "mars-rover-photos-database")
                .build()
    }

}