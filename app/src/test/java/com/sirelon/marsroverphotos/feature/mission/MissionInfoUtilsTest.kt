package com.sirelon.marsroverphotos.feature.mission

import com.sirelon.marsroverphotos.models.Rover
import org.junit.Assert.assertEquals
import org.junit.Test

class MissionInfoUtilsTest {

    @Test
    fun calculateEarthDaysActive_returnsInclusiveDays() {
        val days = calculateEarthDaysActive(
            landingDate = "2021-02-18",
            maxDate = "2021-02-20"
        )

        assertEquals(3L, days)
    }

    @Test
    fun calculateEarthDaysActive_returnsZeroForInvalidDates() {
        val days = calculateEarthDaysActive(
            landingDate = "invalid",
            maxDate = "2021-02-20"
        )

        assertEquals(0L, days)
    }

    @Test
    fun buildTimelineMilestones_activeRover_hasThreeMilestones() {
        val rover = buildRover(status = "active")

        val milestones = buildTimelineMilestones(rover)

        assertEquals(3, milestones.size)
        assertEquals(MilestoneType.LAUNCH, milestones[0].type)
        assertEquals(MilestoneType.LANDING, milestones[1].type)
        assertEquals(MilestoneType.CURRENT, milestones[2].type)
    }

    @Test
    fun buildTimelineMilestones_completeRover_includesEnd() {
        val rover = buildRover(status = "complete")

        val milestones = buildTimelineMilestones(rover)

        assertEquals(4, milestones.size)
        assertEquals(MilestoneType.END, milestones.last().type)
        assertEquals("End", milestones.last().label)
    }

    private fun buildRover(status: String): Rover {
        return Rover(
            id = 1L,
            name = "Test Rover",
            drawableName = "img_test",
            landingDate = "2021-02-18",
            launchDate = "2020-07-30",
            status = status,
            maxSol = 10L,
            maxDate = "2021-02-20",
            totalPhotos = 0
        )
    }
}
