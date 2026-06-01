package com.sirelon.marsroverphotos.data.database.dao

import androidx.paging.PagingSource
import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.data.database.entities.StatsUpdate
import kotlinx.coroutines.flow.Flow

/**
 * Created on 22.08.2020 17:42 for Mars-Rover-Photos.
 */
@Dao
@Suppress("TooManyFunctions")
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
    suspend fun updateStats(stats: StatsUpdate)

    @Query("SELECT * FROM images WHERE id IN (:ids)")
    suspend fun loadImagesByIds(ids: List<String>): List<MarsImage>

    @Query("SELECT * FROM images WHERE favorite = 1 ORDER BY `order` ASC")
    fun loadFavoriteImages(): Flow<List<MarsImage>>

    @Query("SELECT * FROM images WHERE popular = 1 ORDER BY `order` ASC")
    fun loadPopularImages(): Flow<List<MarsImage>>

    // room-paging supports all KMP targets since room3 3.0.0-alpha05 (b/339934824)
    @Query("SELECT * FROM images WHERE favorite = 1 ORDER BY `order` ASC")
    fun loadFavoritePagedSource(): PagingSource<Int, MarsImage>

    @Query("SELECT * FROM images WHERE popular = 1 ORDER BY `order` ASC")
    fun loadPopularPagedSource(): PagingSource<Int, MarsImage>

    @Query("DELETE FROM images WHERE popular = 1")
    suspend fun deleteAllPopular()

    /**
     * Evicts cached (non-favorite, non-popular) images, keeping the [keepCount] most-recently
     * cached rows. Recency is expressed by SQLite's implicit `rowid` (monotonic with insertion):
     * the `order` column is reset per-sol so it does not express recency across sols.
     */
    @Query("""
        DELETE FROM images
        WHERE favorite = 0
          AND popular = 0
          AND id NOT IN (
            SELECT id FROM images
            WHERE favorite = 0 AND popular = 0
            ORDER BY rowid DESC
            LIMIT :keepCount
          )
    """)
    suspend fun deleteNonUserImagesBeyondCount(keepCount: Int)

    // TODO: Re-enable when Room KMP fixes Kotlin/Native @Transaction support
    // Do not name it 'withTransaction' see more here: https://issuetracker.google.com/issues/275678088
    // @Transaction
    // suspend fun withDaoTransaction(action: suspend () -> Unit) {
    //     action.invoke()
    // }
}
