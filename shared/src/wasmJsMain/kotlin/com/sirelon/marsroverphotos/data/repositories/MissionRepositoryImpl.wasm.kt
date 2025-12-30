package com.sirelon.marsroverphotos.data.repositories

import com.sirelon.marsroverphotos.domain.models.mission.RoverMissionFacts
import com.sirelon.marsroverphotos.domain.repositories.MissionRepository

/**
 * Web implementation of MissionRepository.
 * TODO: Implement with Firebase Web SDK
 */
class MissionRepositoryImpl : MissionRepository {
    override suspend fun getRoverMissionFacts(roverId: Long): RoverMissionFacts? {
        // TODO: Implement with Firebase Web SDK
        return null
    }
}
