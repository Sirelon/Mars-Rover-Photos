package com.sirelon.marsroverphotos.storage

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * Created on 22.08.2020 17:42 for Mars-Rover-Photos.
 */
@Dao
interface ImagesDao {
    // Emits the number of users added to the database.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertImages(images: List<MarsImage>)

    @Query("SELECT * FROM images WHERE favorite = 1")
    fun getFavoriteImages(): LiveData<List<MarsImage>>

    @Query("SELECT * FROM images c LIMIT 1")
    fun getOneImage(): LiveData<MarsImage?>

    @Query("SELECT * FROM images WHERE id IN (:ids)")
    fun getImagesByIds(ids: List<Int>): LiveData<List<MarsImage>>

    @Update
    fun update(item: MarsImage)

    @Query("SELECT * FROM images")
    fun loadPagedSource(): PagingSource<Int, MarsImage>

}