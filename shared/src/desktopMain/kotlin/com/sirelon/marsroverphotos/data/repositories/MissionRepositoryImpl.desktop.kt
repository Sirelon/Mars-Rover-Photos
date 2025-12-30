package com.sirelon.marsroverphotos.data.repositories

import com.sirelon.marsroverphotos.domain.models.mission.RoverMissionFacts
import com.sirelon.marsroverphotos.domain.repositories.MissionRepository

/**
 * Desktop implementation of MissionRepository.
 * Stub implementation - Firebase not available on desktop.
 */
class MissionRepositoryImpl : MissionRepository {
    override suspend fun getRoverMissionFacts(roverId: Long): RoverMissionFacts? {
        // Firebase not available on desktop
        return null
    }
}
