package com.sirelon.marsroverphotos.domain.repositories

import com.sirelon.marsroverphotos.domain.models.Rover
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing rover metadata.
 * @author romanishin
 * @since 07.11.16 on 12:42
 */
interface RoversRepository {
    /**
     * Initialize the repository by seeding the database and starting background sync tasks.
     * Should be called once on app startup.
     */
    fun initialize()

    /**
     * Get all rovers as a Flow.
     * @return Flow of list of rovers
     */
    fun getRovers(): Flow<List<Rover>>

    /**
     * Load a specific rover by ID.
     * @param id Rover ID
     * @return Rover or null if not found
     */
    suspend fun loadRoverById(id: Long): Rover?

    /**
     * Update the photo count for a specific rover.
     * @param roverId Rover ID
     * @param photos New photo count
     */
    suspend fun updateRoverCountPhotos(roverId: Long, photos: Long)
}
