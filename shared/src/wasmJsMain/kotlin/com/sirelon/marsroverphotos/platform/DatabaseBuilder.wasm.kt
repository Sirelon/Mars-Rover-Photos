package com.sirelon.marsroverphotos.platform

import androidx.room.Room
import androidx.room.RoomDatabase
import com.sirelon.marsroverphotos.data.database.AppDataBase

/**
 * Web database builder.
 * Uses in-memory database for now. Could be enhanced with IndexedDB support.
 */
actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDataBase> {
    // For Web/Wasm, we use in-memory database
    // TODO: Consider implementing IndexedDB support for persistence
    return Room.inMemoryDatabaseBuilder<AppDataBase>()
}
