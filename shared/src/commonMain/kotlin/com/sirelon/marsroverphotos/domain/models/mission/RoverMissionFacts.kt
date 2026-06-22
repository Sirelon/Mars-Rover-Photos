package com.sirelon.marsroverphotos.domain.models.mission

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Data class representing mission facts for a rover stored in Firestore.
 */
@Immutable
@Serializable
data class RoverMissionFacts(
    val roverId: Long = 0,
    val roverName: String = "",
    val objectives: List<String> = emptyList(),
    val funFacts: List<String> = emptyList()
)
