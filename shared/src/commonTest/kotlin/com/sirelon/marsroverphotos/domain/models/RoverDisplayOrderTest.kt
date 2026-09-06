package com.sirelon.marsroverphotos.domain.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The roster is sorted by [ROVER_DISPLAY_ORDER] rather than by the id order the DAO happens to
 * return, so that Ingenuity sits beside the rover it flew with instead of at the bottom of the
 * list. These tests pin that placement and the fallback for anything not listed.
 */
class RoverDisplayOrderTest {

    private fun rover(id: Long) = Rover(
        id = id,
        name = "rover-$id",
        drawableName = "",
        landingDate = "",
        launchDate = "",
        status = "complete",
        maxSol = 0L,
        maxDate = "",
        totalPhotos = 0,
    )

    /** Runs the production comparator, so deleting it from the repository fails these tests. */
    private fun sorted(ids: List<Long>): List<Long> =
        ids.map(::rover).inDisplayOrder().map { it.id }

    @Test
    fun ingenuityFollowsPerseverance() {
        val perseverance = ROVER_DISPLAY_ORDER.indexOf(PERSEVERANCE_ID)
        val ingenuity = ROVER_DISPLAY_ORDER.indexOf(INGENUITY_ID)

        assertEquals(perseverance + 1, ingenuity)
    }

    @Test
    fun everyMissionIsListedExactlyOnce() {
        val all = listOf(
            PERSEVERANCE_ID, INGENUITY_ID, INSIGHT_ID, CURIOSITY_ID,
            OPPORTUNITY_ID, SPIRIT_ID, VIKING_1_ID, VIKING_2_ID,
        )

        assertEquals(all.size, ROVER_DISPLAY_ORDER.size)
        assertEquals(all.toSet(), ROVER_DISPLAY_ORDER.toSet())
        assertEquals(ROVER_DISPLAY_ORDER.size, ROVER_DISPLAY_ORDER.distinct().size)
    }

    @Test
    fun addingIngenuityLeavesTheOtherMissionsInTheirExistingOrder() {
        // Before this change the DAO's unordered SELECT returned rows in ascending id order.
        val previousOrder = listOf(
            PERSEVERANCE_ID, INSIGHT_ID, CURIOSITY_ID, OPPORTUNITY_ID,
            SPIRIT_ID, VIKING_1_ID, VIKING_2_ID,
        )

        assertEquals(previousOrder, ROVER_DISPLAY_ORDER.filter { it != INGENUITY_ID })
    }

    @Test
    fun sortIsStableRegardlessOfTheOrderRowsArriveIn() {
        val shuffled = listOf(VIKING_2_ID, INGENUITY_ID, CURIOSITY_ID, PERSEVERANCE_ID)

        assertEquals(
            listOf(PERSEVERANCE_ID, INGENUITY_ID, CURIOSITY_ID, VIKING_2_ID),
            sorted(shuffled),
        )
    }

    @Test
    fun unknownIdsSortLastRatherThanDisappearing() {
        val withUnknown = listOf(999L, INGENUITY_ID, PERSEVERANCE_ID)

        val result = sorted(withUnknown)

        assertEquals(3, result.size)
        assertTrue(result.last() == 999L)
    }
}
