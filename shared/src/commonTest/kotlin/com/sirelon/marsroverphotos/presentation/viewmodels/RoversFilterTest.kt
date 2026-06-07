package com.sirelon.marsroverphotos.presentation.viewmodels

import com.sirelon.marsroverphotos.domain.models.Rover
import kotlin.test.Test
import kotlin.test.assertEquals

private fun rover(id: Long, name: String) = Rover(
    id = id,
    name = name,
    drawableName = "",
    landingDate = "",
    launchDate = "",
    status = "active",
    maxSol = 0L,
    maxDate = "",
    totalPhotos = 0
)

class RoversFilterTest {

    private val curiosity = rover(5L, "Curiosity")
    private val opportunity = rover(6L, "Opportunity")
    private val spirit = rover(7L, "Spirit")
    private val insight = rover(4L, "InSight")
    private val perseverance = rover(3L, "Perseverance")
    private val all = listOf(curiosity, opportunity, spirit, insight, perseverance)

    @Test
    fun blankQuery_returnsAll() {
        assertEquals(all, filterRovers(all, ""))
    }

    @Test
    fun whitespaceQuery_returnsAll() {
        assertEquals(all, filterRovers(all, "   "))
    }

    @Test
    fun exactMatch_returnsSingleRover() {
        assertEquals(listOf(curiosity), filterRovers(all, "Curiosity"))
    }

    @Test
    fun caseInsensitive_matches() {
        assertEquals(listOf(curiosity), filterRovers(all, "curiosity"))
    }

    @Test
    fun partialQuery_returnsSubset() {
        // "ity" matches Curiosity and Opportunity
        assertEquals(listOf(curiosity, opportunity), filterRovers(all, "ity"))
    }

    @Test
    fun noMatch_returnsEmpty() {
        assertEquals(emptyList(), filterRovers(all, "Viking"))
    }
}
