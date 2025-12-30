package com.sirelon.marsroverphotos.data.repositories

import com.sirelon.marsroverphotos.data.database.dao.FactDisplayDao
import com.sirelon.marsroverphotos.data.database.entities.FactDisplay
import com.sirelon.marsroverphotos.domain.models.EducationalFact
import com.sirelon.marsroverphotos.domain.repositories.FactsRepository
import com.sirelon.marsroverphotos.platform.IFirebasePhotos
import com.sirelon.marsroverphotos.utils.Logger
import com.sirelon.marsroverphotos.utils.RandomGenerator
import kotlinx.datetime.Clock

/**
 * Implementation of FactsRepository.
 * Manages educational facts from Firebase with session-based tracking to avoid repetition.
 */
class FactsRepositoryImpl(
    private val firebasePhotos: IFirebasePhotos,
    private val factDisplayDao: FactDisplayDao
) : FactsRepository {

    private var cachedFacts: List<EducationalFact> = emptyList()
    private var currentSessionId: String = RandomGenerator.randomUUID()

    override suspend fun loadFacts() {
        try {
            cachedFacts = firebasePhotos.loadEducationalFacts()
            Logger.d("FactsRepository") { "Loaded ${cachedFacts.size} educational facts from Firebase" }
        } catch (e: Exception) {
            Logger.e("FactsRepository", e) { "Failed to load educational facts from Firebase" }
            cachedFacts = emptyList()
        }
    }

    override suspend fun getNextFact(): EducationalFact? {
        if (cachedFacts.isEmpty()) {
            Logger.w("FactsRepository") { "No facts available - cache is empty" }
            return null
        }

        val displayedIds = factDisplayDao.getDisplayedFactIds(currentSessionId)
        val unseenFacts = cachedFacts.filter { it.id !in displayedIds }

        return if (unseenFacts.isNotEmpty()) {
            unseenFacts.random()
        } else {
            // All facts shown in this session, reset and start over
            Logger.d("FactsRepository") { "All facts shown, resetting session" }
            factDisplayDao.resetSessionFacts(currentSessionId)
            currentSessionId = RandomGenerator.randomUUID()
            cachedFacts.random()
        }
    }

    override suspend fun markFactAsShown(fact: EducationalFact) {
        val display = FactDisplay(
            factId = fact.id,
            sessionId = currentSessionId
        )
        factDisplayDao.insert(display)
    }

    override suspend fun cleanupOldDisplays() {
        val thirtyDaysAgo = Clock.System.now().toEpochMilliseconds() - (30L * 24 * 60 * 60 * 1000)
        val thirtyDaysAgo = Clock.System.now().toEpochMilliseconds() - (30L * 24 * 60 * 60 * 1000)
        factDisplayDao.cleanupOldDisplays(thirtyDaysAgo)
        Logger.d("FactsRepository") { "Cleaned up old fact display records" }
    }
}
