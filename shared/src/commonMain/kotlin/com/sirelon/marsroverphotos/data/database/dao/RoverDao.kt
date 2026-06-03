package com.sirelon.marsroverphotos.data.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.sirelon.marsroverphotos.domain.models.Rover
import kotlinx.coroutines.flow.Flow

/**
 * Created on 2019-03-14 12:55 for Mars-Rover-Photos.
 */
@Dao
interface RoverDao {

    // Emits the number of users added to the database.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRovers(vararg rovers: Rover)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRoversList(rovers: List<Rover>)

    @Query("UPDATE rover SET totalPhotos = :photos WHERE id = :roverId")
    suspend fun updateRoverCountPhotos(roverId: Long, photos: Long)

    @Query("UPDATE rover SET maxSol = :maxSol WHERE id = :roverId")
    suspend fun updateMaxSol(roverId: Long, maxSol: Long)

    @Query("SELECT * FROM rover")
    fun getRovers(): Flow<List<Rover>>

    @Query("SELECT * FROM rover WHERE id = :id")
    suspend fun loadRoverById(id: Long): Rover?

    @Query("UPDATE rover SET landingDate = :landingDate, launchDate = :launchDate, maxSol = :maxSol, maxDate = :maxDate, totalPhotos = :totalPhotos WHERE name = :name")
    @Suppress("LongParameterList")
    suspend fun updateRover(
        name: String,
        landingDate: String,
        launchDate: String,
        maxSol: Long,
        maxDate: String,
        totalPhotos: Int
    )
}
