package com.sirelon.marsroverphotos.domain.repositories

import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.domain.models.CURIOSITY_ID
import com.sirelon.marsroverphotos.domain.models.INGENUITY_ID
import com.sirelon.marsroverphotos.domain.models.PhotosQueryRequest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The nearest-sol lookup is an optimisation, never a dependency: it rides on an undocumented
 * range operator, so every caller has to keep working when it returns null. These tests pin that
 * contract at the interface level — a repository that cannot answer must say so rather than throw.
 */
class NearestSolFallbackTest {

    /** Answers only for Ingenuity, and only when the underlying lookup succeeds. */
    private class StubRepository(
        private val answer: Long? = null,
        private val failure: Throwable? = null,
    ) : PhotosRepository {
        var calls = 0

        override suspend fun refreshImages(query: PhotosQueryRequest): List<MarsImage> = emptyList()

        override suspend fun nearestSolWithPhotosAtOrAfter(roverId: Long, fromSol: Long): Long? {
            if (roverId != INGENUITY_ID) return null
            calls++
            return runCatching { failure?.let { throw it } ?: answer }.getOrNull()
        }
    }

    /** A repository that never overrides the method at all — the default every other one uses. */
    private class DefaultRepository : PhotosRepository {
        override suspend fun refreshImages(query: PhotosQueryRequest): List<MarsImage> = emptyList()
    }

    @Test
    fun defaultImplementation_returnsNull() = runTest {
        // Viking and the MER rovers never override this; they must compile and answer null.
        assertNull(DefaultRepository().nearestSolWithPhotosAtOrAfter(INGENUITY_ID, 500L))
    }

    @Test
    fun nonIngenuityRover_shortCircuitsWithoutAsking() = runTest {
        val repo = StubRepository(answer = 852L)

        assertNull(repo.nearestSolWithPhotosAtOrAfter(CURIOSITY_ID, 500L))
        assertEquals(0, repo.calls, "dense feeds must not pay for a lookup they don't need")
    }

    @Test
    fun ingenuity_passesTheResolvedSolThrough() = runTest {
        val repo = StubRepository(answer = 852L)

        assertEquals(852L, repo.nearestSolWithPhotosAtOrAfter(INGENUITY_ID, 777L))
    }

    @Test
    fun lookupFailure_surfacesAsNullNotAnException() = runTest {
        // The caller opens the feed at its original sol when this happens; it must not crash.
        val repo = StubRepository(failure = IllegalStateException("network down"))

        assertNull(repo.nearestSolWithPhotosAtOrAfter(INGENUITY_ID, 777L))
    }
}
