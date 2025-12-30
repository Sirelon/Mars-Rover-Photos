package com.sirelon.marsroverphotos.data.repositories

import com.sirelon.marsroverphotos.domain.models.mission.RoverMissionFacts
import com.sirelon.marsroverphotos.domain.repositories.MissionRepository

/**
 * iOS implementation of MissionRepository.
 * TODO: Implement Firebase iOS SDK integration
 */
class MissionRepositoryImpl : MissionRepository {
    override suspend fun getRoverMissionFacts(roverId: Long): RoverMissionFacts? {
        // TODO: Implement with Firebase iOS SDK
        return null
    }
}
