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
}
