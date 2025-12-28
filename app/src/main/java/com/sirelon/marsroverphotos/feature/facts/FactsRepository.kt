package com.sirelon.marsroverphotos.feature.facts

import com.sirelon.marsroverphotos.firebase.photos.IFirebasePhotos
import com.sirelon.marsroverphotos.storage.FactDisplay
import com.sirelon.marsroverphotos.storage.FactDisplayDao
import timber.log.Timber
import java.util.UUID

/**
 * Repository for managing educational facts.
 * Fetches facts from Firebase on app start, caches them in memory,
 * and tracks which facts have been shown to avoid repetition.
 */
class FactsRepository(
    private val firebasePhotos: IFirebasePhotos,
    private val factDisplayDao: FactDisplayDao
) {

    private var cachedFacts: List<EducationalFact> = emptyList()
    private var currentSessionId: String = UUID.randomUUID().toString()

    /**
     * Load facts from Firebase and cache them in memory.
     * Should be called once on app start.
     */
    suspend fun loadFacts() {
        try {
            cachedFacts = firebasePhotos.loadEducationalFacts()
            Timber.d("Loaded ${cachedFacts.size} educational facts from Firebase")
        } catch (e: Exception) {
            Timber.e(e, "Failed to load educational facts from Firebase")
            cachedFacts = emptyList()
        }
    }

    /**
     * Get the next random fact that hasn't been shown in the current session.
     * If all facts have been shown, reset the session and start over.
     * Returns null if no facts are available (e.g., Firebase fetch failed).
     */
    suspend fun getNextFact(): EducationalFact? {
        if (cachedFacts.isEmpty()) {
            Timber.w("No facts available - cache is empty")
            return null
        }

        val displayedIds = factDisplayDao.getDisplayedFactIds(currentSessionId)
        val unseenFacts = cachedFacts.filter { it.id !in displayedIds }

        return if (unseenFacts.isNotEmpty()) {
            unseenFacts.random()
        } else {
            // All facts shown in this session, reset and start over
            Timber.d("All facts shown, resetting session")
            factDisplayDao.resetSessionFacts(currentSessionId)
            currentSessionId = UUID.randomUUID().toString()
            cachedFacts.random()
        }
    }

    /**
     * Mark a fact as shown in the current session.
     * This prevents it from being shown again until all facts have been displayed.
     */
    suspend fun markFactAsShown(fact: EducationalFact) {
        val display = FactDisplay(
            factId = fact.id,
            sessionId = currentSessionId
        )
        factDisplayDao.insert(display)
    }

    /**
     * Clean up old display records (older than 30 days).
     * Helps keep the database size manageable.
     */
    suspend fun cleanupOldDisplays() {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        factDisplayDao.cleanupOldDisplays(thirtyDaysAgo)
        Timber.d("Cleaned up old fact display records")
    }
}
