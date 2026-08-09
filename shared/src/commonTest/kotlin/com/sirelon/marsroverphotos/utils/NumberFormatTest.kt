package com.sirelon.marsroverphotos.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class NumberFormatTest {

    @Test
    fun formatCompact_belowThousand_returnsAsIs() {
        assertEquals("999", formatCompact(999))
    }

    @Test
    fun formatCompact_belowTenThousand_oneDecimalK() {
        assertEquals("1.5K", formatCompact(1505))
    }

    @Test
    fun formatCompact_atOrAboveTenThousand_zeroDecimalK() {
        assertEquals("134K", formatCompact(133811))
    }

    @Test
    fun formatCompact_millions_oneDecimalM() {
        assertEquals("1.2M", formatCompact(1_200_000))
    }

    @Test
    fun formatCompact_roundingCarriesIntoMillions() {
        // Rounding to thousands would give "1000K"; it must promote to the next unit instead.
        assertEquals("1.0M", formatCompact(999_500))
        assertEquals("1.0M", formatCompact(999_999))
        assertEquals("999K", formatCompact(999_499))
    }
}
