package com.sirelon.marsroverphotos.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO for tracking displayed educational facts to avoid repetition.
 */
@Dao
interface FactDisplayDao {

    /**
     * Record that a fact was displayed in the current session.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(factDisplay: FactDisplay)

    /**
     * Get list of fact IDs that have been displayed in the current session.
     */
    @Query("SELECT factId FROM fact_displays WHERE sessionId = :sessionId")
    suspend fun getDisplayedFactIds(sessionId: String): List<String>

    /**
     * Clear all displayed facts for a specific session.
     * Called when all facts have been shown and we want to start over.
     */
    @Query("DELETE FROM fact_displays WHERE sessionId = :sessionId")
    suspend fun resetSessionFacts(sessionId: String)

    /**
     * Clean up old display records (older than cutoff timestamp).
     * Helps keep database size manageable.
     */
    @Query("DELETE FROM fact_displays WHERE displayTimestamp < :cutoffTimestamp")
    suspend fun cleanupOldDisplays(cutoffTimestamp: Long)
}
