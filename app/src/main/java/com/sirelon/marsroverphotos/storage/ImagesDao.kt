package com.sirelon.marsroverphotos.storage

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

    @Query("SELECT * FROM images WHERE id IN (:ids) ORDER BY `order` ASC")
    fun getImagesByIds(ids: List<String>): Flow<List<MarsImage>>

    @Query("SELECT * FROM images")
    fun getAllImages(): Flow<List<MarsImage>>

    @Update(entity = MarsImage::class)
    suspend fun update(item: MarsImage)

    @Query("UPDATE images SET favorite = :favorite, counter_favorite = :counter WHERE id = :id")
    suspend fun updateFavorite(id: String, favorite: Boolean, counter: Long)

    @Update(entity = MarsImage::class)
    fun updateStats(stats: StatsUpdate)

    @Query("SELECT * FROM images WHERE id IN (:ids)")
    suspend fun loadImagesByIds(ids: List<String>): List<MarsImage>

    @Query("SELECT * FROM images WHERE favorite = 1 ORDER BY `order` ASC")
    fun loadFavoritePagedSource(): PagingSource<Int, MarsImage>

    @Query("SELECT * FROM images WHERE popular = 1 ORDER BY `order` ASC")
    fun loadPopularPagedSource(): PagingSource<Int, MarsImage>

    @Query("DELETE FROM images WHERE popular = 1")
    fun deleteAllPopular()

    // Do not name it 'withTransaction' see more here: https://issuetracker.google.com/issues/275678088
    @Transaction
    suspend fun withDaoTransaction(action: suspend () -> Unit) {
        action.invoke()
    }

}