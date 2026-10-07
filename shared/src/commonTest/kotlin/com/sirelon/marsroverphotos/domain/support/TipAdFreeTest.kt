package com.sirelon.marsroverphotos.domain.support

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days

class TipAdFreeTest {

    private val day = 1.days.inWholeMilliseconds

    @Test
    fun noTipsMeansNoWindow() {
        assertNull(tipAdFreeUntil(emptyList()))
    }

    @Test
    fun oneTipLastsThirtyDays() {
        assertEquals(30 * day, tipAdFreeUntil(listOf(0L)))
    }

    @Test
    fun tipInsideOpenWindowExtendsIt() {
        assertEquals(60 * day, tipAdFreeUntil(listOf(10 * day, 0L)))
    }

    @Test
    fun tipAfterWindowClosedStartsFresh() {
        assertEquals(130 * day, tipAdFreeUntil(listOf(0L, 100 * day)))
    }
}
