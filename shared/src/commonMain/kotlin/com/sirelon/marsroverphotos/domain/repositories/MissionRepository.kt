package com.sirelon.marsroverphotos.domain.repositories

import com.sirelon.marsroverphotos.domain.models.mission.RoverMissionFacts

/**
 * Repository interface for fetching rover mission facts.
 */
interface MissionRepository {
    /**
     * Get mission facts for a specific rover.
     * @param roverId The ID of the rover
     * @return RoverMissionFacts or null if not found or error occurred
     */
    suspend fun getRoverMissionFacts(roverId: Long): RoverMissionFacts?
}
