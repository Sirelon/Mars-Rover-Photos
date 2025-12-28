package com.sirelon.marsroverphotos.firebase.mission

import kotlinx.serialization.Serializable

/**
 * Interface for fetching rover mission facts from Firebase Firestore.
 */
interface IFirebaseMission {
    /**
     * Get mission facts for a specific rover.
     * @param roverId The ID of the rover
     * @return RoverMissionFacts or null if not found or error occurred
     */
    suspend fun getRoverMissionFacts(roverId: Long): RoverMissionFacts?
}

/**
 * Data class representing mission facts for a rover stored in Firestore.
 */
@Serializable
data class RoverMissionFacts(
    val roverId: Long = 0,
    val roverName: String = "",
    val objectives: List<String> = emptyList(),
    val funFacts: List<String> = emptyList()
)
