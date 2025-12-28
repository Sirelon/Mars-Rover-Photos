package com.sirelon.marsroverphotos.firebase.mission

import org.junit.Assert.assertEquals
import org.junit.Test

class MissionFactsMapperTest {

    @Test
    fun mapMissionFacts_usesProvidedValuesAndFiltersLists() {
        val data = mapOf(
            "roverId" to 5L,
            "roverName" to "Curiosity",
            "objectives" to listOf("Obj1", 2, "Obj2"),
            "funFacts" to listOf("Fact1")
        )

        val facts = mapMissionFacts(roverId = 99L, data = data)

        assertEquals(5L, facts.roverId)
        assertEquals("Curiosity", facts.roverName)
        assertEquals(listOf("Obj1", "Obj2"), facts.objectives)
        assertEquals(listOf("Fact1"), facts.funFacts)
    }

    @Test
    fun mapMissionFacts_defaultsWhenMissingData() {
        val data = emptyMap<String, Any?>()

        val facts = mapMissionFacts(roverId = 7L, data = data)

        assertEquals(7L, facts.roverId)
        assertEquals("", facts.roverName)
        assertEquals(emptyList<String>(), facts.objectives)
        assertEquals(emptyList<String>(), facts.funFacts)
    }
}
