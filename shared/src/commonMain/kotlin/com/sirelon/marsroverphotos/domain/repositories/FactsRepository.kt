package com.sirelon.marsroverphotos.domain.repositories

import com.sirelon.marsroverphotos.domain.models.EducationalFact

/**
 * Repository for managing educational facts.
 * Fetches facts from Firebase on app start, caches them in memory,
 * and tracks which facts have been shown to avoid repetition.
 */
interface FactsRepository {
    /**
     * Load facts from remote source and cache them in memory.
     * Should be called once on app start.
     */
    suspend fun loadFacts()

    /**
     * Get the next random fact that hasn't been shown in the current session.
     * If all facts have been shown, reset the session and start over.
     * Returns null if no facts are available (e.g., remote fetch failed).
     */
    suspend fun getNextFact(): EducationalFact?

    /**
     * Mark a fact as shown in the current session.
     * This prevents it from being shown again until all facts have been displayed.
     */
    suspend fun markFactAsShown(fact: EducationalFact)

    /**
     * Clean up old display records (older than 30 days).
     * Helps keep the database size manageable.
     */
    suspend fun cleanupOldDisplays()
}
