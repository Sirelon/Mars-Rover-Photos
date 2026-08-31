package com.sirelon.marsroverphotos.data.viking

import com.sirelon.marsroverphotos.data.network.RestApi
import com.sirelon.marsroverphotos.data.repositories.PhotosRepositoryImpl
import com.sirelon.marsroverphotos.domain.models.CURIOSITY_ID
import com.sirelon.marsroverphotos.domain.models.PhotosQueryRequest
import com.sirelon.marsroverphotos.domain.models.VIKING_1_ID
import com.sirelon.marsroverphotos.domain.models.VIKING_2_ID
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Exercises the real bundled catalogue resources end to end, and the routing that puts them in
 * front of the network for the two Viking landers.
 *
 * Counts here are the archive's, verified against the live PDS listings by
 * `scripts/generate-viking-catalog.mjs` — if one of them changes, the generator was re-run against
 * different data and that deserves a look rather than a test edit.
 */
class VikingCatalogTest {

    private val catalog = VikingCatalog()

    @Test
    fun `serves the first photograph taken from the surface of Mars`() = runTest {
        val sol0 = catalog.photosForSol(VIKING_1_ID, sol = 0, camera = null)

        assertEquals(2, sol0.size)
        val first = sol0.first()
        assertEquals("12A001-BB1", first.id)
        assertEquals("First Lander 1 Image", first.name)
        assertEquals("1976-07-20", first.earthDate)
        assertTrue(first.imageUrl.endsWith("/a0xx/12a001b1.jpeg"), first.imageUrl)
    }

    @Test
    fun `both landers carry their browsable archive`() = runTest {
        // The full archive is 3542 / 3043. The generator drops sub-100px instrument readings and
        // a handful of blank diagnostic frames — see MIN_BROWSABLE_WIDTH and
        // NON_PHOTOGRAPHIC_NOTES in scripts/generate-viking-catalog.mjs.
        assertEquals(1333, countPhotos(VIKING_1_ID))
        assertEquals(1581, countPhotos(VIKING_2_ID))
    }

    @Test
    fun `unviewable instrument frames are absent`() = runTest {
        val allViking1 = (0L..2238L).flatMap { catalog.photosForSol(VIKING_1_ID, it, camera = null) }

        // 11A019-N07 is a 60px-wide internal calibration frame — in the PDS index, not the feed.
        assertTrue(allViking1.none { it.id == "11A019-N07" })

        // "Erase Image" is a camera erase cycle: a black rectangle at any width.
        assertTrue(allViking1.none { it.name == "Erase Image" })
        assertTrue(allViking1.none { it.name == "Radio Science Experiment" })

        // The cut is on width, not on subject: the same observation swept across more azimuth is
        // a real picture and stays. 679 optical-depth frames exist across both landers, 22-301px
        // wide, and the 25 widest survive — so filtering by caption would be wrong.
        assertTrue(allViking1.any { it.name == "Morning Atmospheric Optical Depth Measurement" })
    }

    @Test
    fun `sols outside the mission are empty rather than an error`() = runTest {
        // SolPagingSource scans past empty sols, so this is the common case, not an edge case.
        assertTrue(catalog.photosForSol(VIKING_1_ID, sol = 921 + 1, camera = null).isEmpty())
        assertTrue(catalog.photosForSol(VIKING_1_ID, sol = 99_999, camera = null).isEmpty())
    }

    @Test
    fun `filters a sol down to one camera`() = runTest {
        // Viking 2 sol 3 is the first day it used both cameras: 3 frames on VLC1, 1 on VLC2.
        val all = catalog.photosForSol(VIKING_2_ID, sol = 3, camera = null)
        val vlc1 = catalog.photosForSol(VIKING_2_ID, sol = 3, camera = "VLC1")
        val vlc2 = catalog.photosForSol(VIKING_2_ID, sol = 3, camera = "VLC2")

        assertEquals(4, all.size)
        assertEquals(3, vlc1.size)
        assertEquals(1, vlc2.size)
        assertTrue(vlc1.all { it.camera?.name == "VLC1" })
    }

    @Test
    fun `a camera that took nothing that sol filters to empty`() = runTest {
        // Landing day was Camera 2 only on both landers — a real gap, and one the feed has to
        // scan past rather than treat as the end of the mission.
        assertEquals(6, catalog.photosForSol(VIKING_2_ID, sol = 0, camera = "VLC2").size)
        assertTrue(catalog.photosForSol(VIKING_2_ID, sol = 0, camera = "VLC1").isEmpty())
    }

    @Test
    fun `a random photo comes from the mission`() = runTest {
        val photo = assertNotNull(catalog.randomPhoto(VIKING_2_ID))

        assertEquals(VIKING_2_ID, photo.roverId)
        assertTrue(photo.imageUrl.contains("/vl_0002/"), photo.imageUrl)
    }

    @Test
    fun `the repository routes Viking to the catalogue and everything else to the api`() = runTest {
        val repository = PhotosRepositoryImpl(api = RestApi(), vikingCatalog = catalog)

        val viking = repository.refreshImages(PhotosQueryRequest(VIKING_1_ID, sol = 0, camera = null))
        assertEquals(2, viking.size)

        // RestApi rejects ids it has no endpoint for, which is how we know the call reached it
        // rather than being swallowed by the Viking branch. No network request is made.
        assertFailsWith<IllegalArgumentException> {
            repository.refreshImages(PhotosQueryRequest(roverId = 999L, sol = 0, camera = null))
        }
        assertTrue(CURIOSITY_ID != VIKING_1_ID)
    }

    private suspend fun countPhotos(roverId: Long): Int =
        (0L..2500L).sumOf { catalog.photosForSol(roverId, it, camera = null).size }
}
