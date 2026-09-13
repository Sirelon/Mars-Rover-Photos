package com.sirelon.marsroverphotos.data.paging

import androidx.paging.PagingSource
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.domain.models.INGENUITY_ID
import com.sirelon.marsroverphotos.domain.models.mission.RoverMissionData
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Ingenuity is the app's only genuinely sparse sol feed: its images sit on 162 of the 1,027 sols
 * it spent on Mars, and the widest gap between two photographed sols runs 76. These tests pin the
 * paging behaviour that sparseness exposes, using the real measured shape of the archive — the
 * first sol (43), the last (1069), and the 776 -> 852 gap.
 */
class IngenuityFeedTest {

    private companion object {
        const val FIRST_SOL = 43L
        const val LAST_SOL = 1069L

        /** The two ends of the archive's widest gap. Nothing was photographed between them. */
        const val GAP_START = 776L
        const val GAP_END = 852L
    }

    /**
     * Sols either side of the widest gap, plus the first and last sols of the campaign.
     *
     * Every camera is named "UNK" on purpose: that is the `filter_name` the live feed reports for
     * both of Ingenuity's cameras, so a chip filter can never match on the name and has to resolve
     * through the CameraSpec prefix instead. Naming these "NAVCAM"/"RTE" would let the filter
     * short-circuit on a branch production never reaches, and the tests would pass regardless of
     * whether the specs were right.
     */
    private val archive: Map<Long, List<MarsImage>> = mapOf(
        FIRST_SOL to listOf(marsImage("first", FIRST_SOL, "UNK", "HELI_NAV")),
        GAP_START to listOf(marsImage("gapStart", GAP_START, "UNK", "HELI_NAV")),
        GAP_END to listOf(
            marsImage("gapEndNav", GAP_END, "UNK", "HELI_NAV"),
            marsImage("gapEndRte", GAP_END, "UNK", "HELI_RTE"),
        ),
        LAST_SOL to listOf(marsImage("last", LAST_SOL, "UNK", "HELI_NAV")),
    )

    private fun source(
        cameras: Set<String> = emptySet(),
        initialSol: Long = FIRST_SOL,
        repo: FakePhotosRepository = FakePhotosRepository(archive),
    ) = SolPagingSource(
        photosRepository = repo,
        imagesDao = FakeImagesDao(),
        roverId = INGENUITY_ID,
        cameras = cameras,
        initialSol = initialSol,
        minSol = FIRST_SOL,
        maxSol = LAST_SOL,
    )

    private fun page(result: PagingSource.LoadResult<Long, MarsImage>) =
        assertIs<PagingSource.LoadResult.Page<Long, MarsImage>>(result)

    @Test
    fun minSol_isFirstImagedSolNotLanding() {
        // It rode down folded under Perseverance and returned nothing until sol 43, the day it
        // was set down, so the sols before that are permanently empty and must not be offered.
        assertEquals(FIRST_SOL, RoverMissionData.getMinSol(INGENUITY_ID))
    }

    @Test
    fun append_crossesWidestGapInOneLoad() = runTest {
        val src = source()

        val result = src.load(PagingSource.LoadParams.Append(GAP_START + 1, 5, false))

        // 76 empty sols is well inside the unfiltered scan budget, so this resolves to a real
        // page rather than an empty continuation.
        val p = page(result)
        assertEquals(listOf("gapEndNav", "gapEndRte"), p.data.map { it.id })
        assertEquals(GAP_END + 1, p.nextKey)
    }

    @Test
    fun append_pastLastSol_terminates() = runTest {
        val src = source()

        val result = src.load(PagingSource.LoadParams.Append(LAST_SOL + 1, 5, false))

        val p = page(result)
        assertTrue(p.data.isEmpty())
        assertNull(p.nextKey)
    }

    @Test
    fun prepend_belowFirstSol_terminatesAtFirstSolNotZero() = runTest {
        val repo = FakePhotosRepository(archive)
        val src = source(repo = repo)

        val result = src.load(PagingSource.LoadParams.Prepend(FIRST_SOL - 1, 5, false))

        val p = page(result)
        assertTrue(p.data.isEmpty())
        assertNull(p.prevKey)
        // The sols before Ingenuity was set down are outside minSol, so the scan must not probe
        // them one network request at a time on its way down to zero.
        assertTrue(repo.probedSols.none { it < FIRST_SOL }, "probed below minSol: ${repo.probedSols}")
    }

    @Test
    fun colourFilteredFeed_stillCrossesWidestGap() = runTest {
        // HELI_RTE is a small fraction of the archive, so a colour-only feed leans hardest on the
        // empty-sol scan. It must still reach the next colour frame rather than stalling.
        val src = source(cameras = setOf("RTE"))

        val result = src.load(PagingSource.LoadParams.Append(GAP_START, 5, false))

        val p = page(result)
        assertEquals(listOf("gapEndRte"), p.data.map { it.id })
    }

    @Test
    fun cameraSpecs_prefixTheRawInstrumentNames() {
        // The camera filter matches image.camera.fullName against CameraSpec.fullName by prefix,
        // so these must stay the raw instrument strings the feed reports, not the chip labels.
        val specs = RoverMissionData.getCamerasForRover(INGENUITY_ID)

        assertEquals(2, specs.size)
        assertTrue(specs.any { "HELI_NAV".startsWith(it.fullName) }, "no spec matches HELI_NAV")
        assertTrue(specs.any { "HELI_RTE".startsWith(it.fullName) }, "no spec matches HELI_RTE")
        assertEquals(listOf("NAVCAM", "RTE"), specs.map { it.name })
    }
}
