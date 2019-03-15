package com.sirelon.marsroverphotos.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sirelon.marsroverphotos.models.Rover
import io.reactivex.Flowable

/**
 * Created on 2019-03-14 12:55 for Mars-Rover-Photos.
 */
@Dao
interface RoverDao {

    // Emits the number of users added to the database.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRovers(vararg rovers: Rover)

    @Query("UPDATE rover SET totalPhotos = :photos WHERE id = :roverId")
    fun updateRoverCountPhotos(roverId: Long, photos: Long)

    @Query("SELECT * FROM rover ORDER by id DESC")
    fun getRoversFlowable(): Flowable<List<Rover>>

    @Query("UPDATE rover SET landingDate = :landingDate AND launchDate = :launchDate AND maxSol = :maxSol AND maxDate = :maxDate AND totalPhotos = :totalPhotos WHERE name = :name")
    fun updateRover(
        name: String,
        landingDate: String,
        launchDate: String,
        maxSol: Long,
        maxDate: String,
        totalPhotos: Int
    )

}