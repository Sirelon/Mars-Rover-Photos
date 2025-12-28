package com.sirelon.marsroverphotos.firebase.mission

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * Firestore implementation for fetching rover mission facts.
 * Collection: rover-missions
 * Document ID: Rover ID (e.g., "1" for Curiosity)
 */
class FirestoreMission : IFirebaseMission {

    private val firestore = FirebaseFirestore.getInstance()
    private val missionCollection = firestore.collection("rover-missions")

    override suspend fun getRoverMissionFacts(roverId: Long): RoverMissionFacts? {
        return try {
            val doc = missionCollection.document(roverId.toString()).get().await()

            if (!doc.exists()) {
                Timber.w("No mission facts found for rover ID: $roverId")
                return null
            }

            val data = doc.data.orEmpty()
            val facts = mapMissionFacts(roverId, data)

            Timber.d("Successfully fetched mission facts for ${facts.roverName}")
            facts
        } catch (e: Exception) {
            Timber.e(e, "Error fetching mission facts for rover ID: $roverId")
            null
        }
    }
}
