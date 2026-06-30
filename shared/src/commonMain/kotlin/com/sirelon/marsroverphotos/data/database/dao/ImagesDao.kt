package com.sirelon.marsroverphotos.data.database.dao

import androidx.paging.PagingSource
import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.data.database.entities.PopularUpdate
import com.sirelon.marsroverphotos.domain.repositories.FavoriteCounts
import com.sirelon.marsroverphotos.domain.repositories.RoverCount
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

    // `order` is not unique (it restarts per sol / per popular batch), so every ordered query
    // adds `id` as a tiebreaker — offset-based paging over a non-deterministic tie order can
    // duplicate or skip items across page boundaries.
    @Query("SELECT * FROM images WHERE id IN (:ids) ORDER BY `order` ASC, id ASC")
    fun getImagesByIds(ids: List<String>): Flow<List<MarsImage>>

    @Query("SELECT * FROM images")
    fun getAllImages(): Flow<List<MarsImage>>

    @Update(entity = MarsImage::class)
    suspend fun update(item: MarsImage)

    @Query("UPDATE images SET favorite = :favorite, counter_favorite = :counter WHERE id = :id")
    suspend fun updateFavorite(id: String, favorite: Boolean, counter: Long)

    @Update(entity = MarsImage::class)
    suspend fun updatePopular(update: PopularUpdate)

    @Query("SELECT * FROM images WHERE id IN (:ids)")
    suspend fun loadImagesByIds(ids: List<String>): List<MarsImage>

    @Query("SELECT * FROM images WHERE favorite = 1 ORDER BY `order` ASC, id ASC")
    fun loadFavoriteImages(): Flow<List<MarsImage>>

    @Query("SELECT * FROM images WHERE popular = 1 ORDER BY `order` ASC, id ASC")
    fun loadPopularImages(): Flow<List<MarsImage>>

    // room-paging supports all KMP targets since room3 3.0.0-alpha05 (b/339934824)
    // roverId = null means all rovers; non-null filters to the specified rover.
    @Query("SELECT * FROM images WHERE favorite = 1 AND (:roverId IS NULL OR roverId = :roverId) ORDER BY `order` ASC, id ASC")
    fun loadFavoritePagedRecent(roverId: Long?): PagingSource<Int, MarsImage>

    @Query("SELECT * FROM images WHERE favorite = 1 AND (:roverId IS NULL OR roverId = :roverId) ORDER BY counter_see DESC, id ASC")
    fun loadFavoritePagedByViews(roverId: Long?): PagingSource<Int, MarsImage>

    @Query("SELECT * FROM images WHERE favorite = 1 AND (:roverId IS NULL OR roverId = :roverId) ORDER BY camera_name ASC, id ASC")
    fun loadFavoritePagedByCamera(roverId: Long?): PagingSource<Int, MarsImage>

    @Query("SELECT roverId, COUNT(*) as count FROM images WHERE favorite = 1 GROUP BY roverId")
    fun loadFavoriteRoverCounts(): Flow<List<RoverCount>>

    @Query("SELECT COUNT(*) as saved, COUNT(DISTINCT roverId) as roverCount, COUNT(DISTINCT camera_name) as cameraCount FROM images WHERE favorite = 1")
    fun loadFavoriteCounts(): Flow<FavoriteCounts>

    @Query("SELECT id FROM images WHERE favorite = 1 AND (:roverId IS NULL OR roverId = :roverId) ORDER BY `order` ASC, id ASC")
    suspend fun loadFavoriteIdsRecent(roverId: Long?): List<String>

    @Query("SELECT id FROM images WHERE favorite = 1 AND (:roverId IS NULL OR roverId = :roverId) ORDER BY counter_see DESC, id ASC")
    suspend fun loadFavoriteIdsByViews(roverId: Long?): List<String>

    @Query("SELECT id FROM images WHERE favorite = 1 AND (:roverId IS NULL OR roverId = :roverId) ORDER BY camera_name ASC, id ASC")
    suspend fun loadFavoriteIdsByCamera(roverId: Long?): List<String>

    @Query("SELECT * FROM images WHERE popular = 1 ORDER BY `order` ASC, id ASC")
    fun loadPopularPagedSource(): PagingSource<Int, MarsImage>

    /**
     * Resets the popular flag on a REFRESH instead of deleting rows: deleting would drop
     * favorited photos and rows cached by the sol feed. Flag-reset preserves them;
     * [deleteNonUserImagesBeyondCount] handles cleanup of no-longer-referenced rows.
     */
    @Query("UPDATE images SET popular = 0 WHERE popular = 1")
    suspend fun clearPopularFlags()

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
