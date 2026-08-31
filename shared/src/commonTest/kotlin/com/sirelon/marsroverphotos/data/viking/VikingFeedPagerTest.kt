package com.sirelon.marsroverphotos.data.viking

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.testing.TestPager
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.data.network.RestApi
import com.sirelon.marsroverphotos.data.paging.FakeImagesDao
import com.sirelon.marsroverphotos.data.paging.SolPagingSource
import com.sirelon.marsroverphotos.data.repositories.PhotosRepositoryImpl
import com.sirelon.marsroverphotos.domain.models.VIKING_1_ID
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Drives the real bundled catalogue through the real [SolPagingSource], the way the photos feed
 * does. The point is the claim the whole Viking design rests on: because the archive carries a sol
 * per photo, Viking needs no paging code of its own.
 *
 * Viking 1 has browsable photos on only 374 of its 2,231 sols, so scanning across empty sols is the normal case
 * here rather than an edge case.
 */
class VikingFeedPagerTest {

    // Mirrors the production config in RoverFeedPager.
    private val config = PagingConfig(pageSize = 20, prefetchDistance = 10, enablePlaceholders = false)

    private fun source(initialSol: Long, cameras: Set<String> = emptySet()) = SolPagingSource(
        photosRepository = PhotosRepositoryImpl(api = RestApi(), vikingCatalog = VikingCatalog()),
        imagesDao = FakeImagesDao(),
        roverId = VIKING_1_ID,
        cameras = cameras,
        initialSol = initialSol,
        minSol = 0,
        maxSol = 2230,
    )

    private fun page(result: PagingSource.LoadResult<Long, MarsImage>?) =
        assertIs<PagingSource.LoadResult.Page<Long, MarsImage>>(result)

    @Test
    fun `refresh at landing day serves the first photographs from Mars`() = runTest {
        val pager = TestPager(config, source(initialSol = 0))

        val refreshed = page(pager.refresh())

        assertEquals(listOf("12A001-BB1", "12A002-SUR"), refreshed.data.map { it.id })
        assertNull(refreshed.prevKey, "sol 0 is the start of the mission")
    }

    @Test
    fun `refresh on an empty sol scans to the nearest sol with photos`() = runTest {
        // Sol 922 sits inside Viking 1's longest imaging gap (88 sols after sol 921).
        val pager = TestPager(config, source(initialSol = 922))

        val refreshed = page(pager.refresh())

        assertTrue(refreshed.data.isNotEmpty(), "refresh must never land on an empty page")
        val sol = refreshed.data.first().sol
        assertTrue(sol in 921L..1010L, "expected a sol either side of the gap, got $sol")
    }

    @Test
    fun `appending walks forward across empty sols`() = runTest {
        val pager = TestPager(config, source(initialSol = 0))
        page(pager.refresh())

        val sols = buildList {
            repeat(5) {
                val appended = page(pager.append())
                assertTrue(appended.data.isNotEmpty(), "no continuation page is expected in this range")
                add(appended.data.first().sol)
            }
        }

        assertEquals(sols.sorted(), sols, "appended sols must increase")
        assertEquals(sols.distinct(), sols, "each page is a distinct sol")
    }

    @Test
    fun `appending past the last imaged sol terminates pagination`() = runTest {
        // Viking 1's last browsable image is sol 2230, which is also its seeded maxSol.
        val pager = TestPager(config, source(initialSol = 2230))

        val refreshed = page(pager.refresh())
        assertEquals(2230L, refreshed.data.first().sol)
        assertNull(refreshed.nextKey, "sol 2230 is the end of the mission")
    }

    @Test
    fun `a camera-filtered feed still advances across its longer gaps`() = runTest {
        // Filtering to one camera stretches Viking 1's longest gap from 88 sols to 148.
        val pager = TestPager(config, source(initialSol = 900, cameras = setOf("VLC1")))
        page(pager.refresh())

        val appended = page(pager.append())

        assertTrue(appended.data.isNotEmpty(), "the 148-sol gap fits inside one load's budget")
        assertTrue(appended.data.all { it.camera?.name == "VLC1" })
    }

    @Test
    fun `a gap past the scan budget resumes through continuation pages`() = runTest {
        // Viking 1 took no VLC2 frame between sols 1009 and 1379 — 370 sols, past
        // SolPagingSource's 300-sol FILTER_LOAD_BUDGET, so this is the one place in either
        // mission that yields an empty continuation page. Paging must walk it to the far side
        // rather than dead-ending, which is the stall this whole mechanism exists to prevent.
        val pager = TestPager(config, source(initialSol = 1009, cameras = setOf("VLC2")))
        page(pager.refresh())

        var appended = page(pager.append())
        var continuations = 0
        while (appended.data.isEmpty() && appended.nextKey != null && continuations < 5) {
            continuations++
            appended = page(pager.append())
        }

        assertTrue(continuations > 0, "expected at least one continuation page across a 370-sol gap")
        assertTrue(appended.data.isNotEmpty(), "the far side of the gap must be reachable")
        assertTrue(appended.data.all { it.camera?.name == "VLC2" })
    }
}
