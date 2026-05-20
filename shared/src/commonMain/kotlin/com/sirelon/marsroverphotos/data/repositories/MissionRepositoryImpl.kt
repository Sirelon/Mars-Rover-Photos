package com.sirelon.marsroverphotos.data.repositories

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import com.sirelon.marsroverphotos.domain.models.mission.RoverMissionFacts
import com.sirelon.marsroverphotos.domain.repositories.MissionRepository
import com.sirelon.marsroverphotos.utils.Logger

class MissionRepositoryImpl : MissionRepository {

    private val firestore get() = Firebase.firestore
    private val missionCollection get() = firestore.collection("rover-missions")

    override suspend fun getRoverMissionFacts(roverId: Long): RoverMissionFacts? {
        return try {
            val doc = missionCollection.document(roverId.toString()).get()
            if (!doc.exists) {
                Logger.w("MissionRepository") { "No mission facts found for rover ID: $roverId" }
                return null
            }
            val data = doc.data<Map<String, Any?>>()
            val facts = RoverMissionFacts(
                roverId = (data["roverId"] as? Number)?.toLong() ?: roverId,
                roverName = data["roverName"] as? String ?: "",
                objectives = parseStringList(data["objectives"]),
                funFacts = parseStringList(data["funFacts"])
            )
            Logger.d("MissionRepository") { "Successfully fetched mission facts for ${facts.roverName}" }
            facts
        } catch (e: Exception) {
            Logger.e("MissionRepository", e) { "Error fetching mission facts for rover ID: $roverId" }
            throw e
        }
    }

    private fun parseStringList(value: Any?): List<String> {
        return when (value) {
            is List<*> -> value.filterIsInstance<String>()
            else -> emptyList()
        }
    }
}
