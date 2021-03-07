package com.sirelon.marsroverphotos.storage

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Created on 22.08.2020 17:42 for Mars-Rover-Photos.
 */
@Dao
interface ImagesDao {
    // Emits the number of users added to the database.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertImages(images: List<MarsImage>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun replaceImages(images: List<MarsImage>)

    @Query("SELECT * FROM images WHERE favorite = 1")
    fun getFavoriteImages(): LiveData<List<MarsImage>>

    @Query("SELECT * FROM images WHERE favorite = 1 LIMIT 1 ")
    fun getOneImage(): LiveData<MarsImage?>

    @Query("SELECT * FROM images WHERE id IN (:ids)")
    fun getImagesByIds(ids: List<String>): LiveData<List<MarsImage>>

    @Update
    fun update(item: MarsImage)

    @Update(entity = MarsImage::class)
    fun updateStats(stats: StatsUpdate)

    @Query("SELECT * FROM images WHERE id IN (:ids)")
    suspend fun loadImagesByIds(ids: List<String>): List<MarsImage>

    @Query("SELECT * FROM images WHERE favorite = 1")
    fun loadFavoritePagedSource(): PagingSource<Int, MarsImage>

    @Query("SELECT * FROM images WHERE popular = 1 ORDER BY `order` ASC")
    fun loadPopularPagedSource(): PagingSource<Int, MarsImage>

    @Query("SELECT * FROM images WHERE popular = 1 ORDER BY `order` ASC")
    fun loadPopularImages(): Flow<List<MarsImage>>

    @Query("DELETE FROM images WHERE popular = 1")
    fun deleteAllPopular()

    @Transaction
    suspend fun withTransaction( action: suspend () -> Unit) {
        action.invoke()
    }

}