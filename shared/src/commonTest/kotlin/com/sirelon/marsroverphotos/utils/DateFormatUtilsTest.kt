package com.sirelon.marsroverphotos.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class DateFormatUtilsTest {

    @Test
    fun plainDate_isFormatted() {
        assertEquals("Aug 5, 2015", formatDisplayDate("2015-08-05"))
    }

    // Regression: the Curiosity feed header showed "2026-11-11T03:37:11.033Z" verbatim.
    @Test
    fun utcTimestamp_showsItsDatePart() {
        assertEquals("Nov 10, 2026", formatDisplayDate("2026-11-10T15:15:47.000Z"))
    }

    // The Mars 2020 feed reports `date_taken_utc` without a zone suffix.
    @Test
    fun timestampWithoutZone_showsItsDatePart() {
        assertEquals("Nov 6, 2026", formatDisplayDate("2026-11-06T12:16:25.848"))
    }

    @Test
    fun unparseableInput_isReturnedUnchanged() {
        assertEquals("not a date", formatDisplayDate("not a date"))
        assertEquals("", formatDisplayDate(""))
    }
}
