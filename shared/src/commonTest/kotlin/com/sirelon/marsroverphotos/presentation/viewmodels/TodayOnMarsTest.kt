package com.sirelon.marsroverphotos.presentation.viewmodels

import com.sirelon.marsroverphotos.domain.models.Rover
import com.sirelon.marsroverphotos.domain.repositories.RoverCoversRepository.Cover
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private fun rover(id: Long, name: String, status: String = "active") = Rover(
    id = id,
    name = name,
    drawableName = "",
    landingDate = "",
    launchDate = "",
    status = status,
    maxSol = 0L,
    maxDate = "",
    totalPhotos = 0,
)

private val perseverance = rover(3L, "Perseverance")
private val curiosity = rover(5L, "Curiosity")
private val spirit = rover(7L, "Spirit", status = "complete")

class TodayOnMarsTest {

    @Test
    fun picksTheFreshestPhotoAmongActiveRovers() {
        val covers = mapOf(
            perseverance.id to Cover("p", 1702, "2026-11-18T23:10:00"),
            curiosity.id to Cover("c", 4710, "2026-11-19T08:00:00.000Z"),
        )

        assertEquals(TodayOnMars(curiosity, covers.getValue(curiosity.id)), pickTodayOnMars(listOf(perseverance, curiosity), covers))
    }

    @Test
    fun comparesTheTimeOnTheSameDay() {
        val covers = mapOf(
            perseverance.id to Cover("p", 1703, "2026-11-19T21:00:00"),
            curiosity.id to Cover("c", 4710, "2026-11-19T08:00:00.000Z"),
        )

        assertEquals(perseverance, pickTodayOnMars(listOf(perseverance, curiosity), covers)?.rover)
    }

    @Test
    fun ignoresClosedMissionsEvenWithANewerDate() {
        val covers = mapOf(
            spirit.id to Cover("s", 2208, "2099-01-01"),
            curiosity.id to Cover("c", 4710, "2026-11-19"),
        )

        assertEquals(curiosity, pickTodayOnMars(listOf(spirit, curiosity), covers)?.rover)
    }

    @Test
    fun skipsCoversWithNoDate() {
        val covers = mapOf(
            perseverance.id to Cover("p"),
            curiosity.id to Cover("c", 4710, "2026-11-19"),
        )

        assertEquals(curiosity, pickTodayOnMars(listOf(perseverance, curiosity), covers)?.rover)
    }

    @Test
    fun isNullWhenNoActiveRoverHasAPhoto() {
        assertNull(pickTodayOnMars(listOf(perseverance, spirit), mapOf(spirit.id to Cover("s", 1, "2010-01-01"))))
    }
}
