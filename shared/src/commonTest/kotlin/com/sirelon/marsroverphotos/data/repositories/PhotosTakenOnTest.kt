package com.sirelon.marsroverphotos.data.repositories

import com.sirelon.marsroverphotos.data.network.RestApi
import com.sirelon.marsroverphotos.data.phoenix.PhoenixCatalog
import com.sirelon.marsroverphotos.data.viking.VikingCatalog
import com.sirelon.marsroverphotos.domain.models.PHOENIX_ID
import com.sirelon.marsroverphotos.domain.models.SPIRIT_ID
import com.sirelon.marsroverphotos.domain.models.VIKING_2_ID
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The real bundled catalogues answered by date through [PhotosRepositoryImpl]. Dates and counts
 * are the catalogue rows; each date spans two sols, so a lookup by sol alone would miss photos.
 */
class PhotosTakenOnTest {

    private val repository = PhotosRepositoryImpl(
        api = RestApi(),
        vikingCatalog = VikingCatalog(),
        phoenixCatalog = PhoenixCatalog(),
    )

    @Test
    fun viking2AnswersEveryRowOfTheDate_acrossTwoSols() = runTest {
        val day = repository.photosTakenOn(VIKING_2_ID, LocalDate(1978, 3, 15))!!

        assertEquals(5, day.total)
        assertEquals(setOf(542L, 543L), day.images.map { it.sol }.toSet())
    }

    @Test
    fun phoenixAnswersEveryRowOfTheDate_acrossTwoSols() = runTest {
        val day = repository.photosTakenOn(PHOENIX_ID, LocalDate(2008, 6, 28))!!

        assertEquals(23, day.total)
        assertEquals(setOf(32L, 33L), day.images.map { it.sol }.toSet())
        assertTrue(day.images.all { it.earthDate.startsWith("2008-06-28") })
    }

    @Test
    fun spiritCannotAnswerByDate() = runTest {
        assertNull(repository.photosTakenOn(SPIRIT_ID, LocalDate(2005, 1, 1)))
    }
}
