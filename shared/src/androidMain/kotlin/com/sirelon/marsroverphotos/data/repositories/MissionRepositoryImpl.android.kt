package com.sirelon.marsroverphotos.data.repositories

import com.google.firebase.firestore.FirebaseFirestore
import com.sirelon.marsroverphotos.domain.models.mission.RoverMissionFacts
import com.sirelon.marsroverphotos.domain.repositories.MissionRepository
import com.sirelon.marsroverphotos.utils.Logger
import kotlinx.coroutines.tasks.await

/**
 * Android implementation of MissionRepository using Firebase Firestore.
 */
class MissionRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : MissionRepository {

    private val missionCollection = firestore.collection("rover-missions")

    override suspend fun getRoverMissionFacts(roverId: Long): RoverMissionFacts? {
        return try {
            val doc = missionCollection.document(roverId.toString()).get().await()

            if (!doc.exists()) {
                Logger.w("MissionRepository") { "No mission facts found for rover ID: $roverId" }
                return null
            }

            val data = doc.data.orEmpty()
            val facts = mapMissionFacts(roverId, data)

            Logger.d("MissionRepository") { "Successfully fetched mission facts for ${facts.roverName}" }
            facts
        } catch (e: Exception) {
            Logger.e("MissionRepository", e) { "Error fetching mission facts for rover ID: $roverId" }
            throw e
        }
    }

    private fun mapMissionFacts(
        roverId: Long,
        data: Map<String, Any?>
    ): RoverMissionFacts {
        return RoverMissionFacts(
            roverId = (data["roverId"] as? Number)?.toLong() ?: roverId,
            roverName = data["roverName"] as? String ?: "",
            objectives = parseStringList(data["objectives"]),
            funFacts = parseStringList(data["funFacts"])
        )
    }

    private fun parseStringList(value: Any?): List<String> {
        return when (value) {
            is List<*> -> value.filterIsInstance<String>()
            else -> emptyList()
        }
    }
}
