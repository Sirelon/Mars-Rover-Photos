package com.sirelon.marsroverphotos.data.phoenix

import com.sirelon.marsroverphotos.domain.models.PHOENIX_ID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The parser against fixtures lifted from the real catalogue. The format is written by
 * `scripts/generate-phoenix-catalog.mjs`.
 */
class PhoenixCatalogParserTest {

    private fun catalogue(vararg rows: String) =
        (listOf("phoenix-catalog v1 rows=${rows.size}") + rows).joinToString("\n") + "\n"

    @Test
    fun `a stereo imager row becomes a fully described photo`() {
        val parsed = PhoenixCatalogParser.parse(catalogue("SS012EFF897271925_11C60L1M1|12|2008-06-07T03:12:45Z|672"))

        val photo = parsed.getValue(12L).single()
        assertEquals("SS012EFF897271925_11C60L1M1", photo.id)
        assertEquals(12L, photo.sol)
        assertEquals(PHOENIX_ID, photo.roverId)
        assertEquals("2008-06-07T03:12:45Z", photo.earthDate)
        assertEquals(
            "https://planetarydata.jpl.nasa.gov/img/data/phoenix/phxssi_0xxx/extras/browse/sol012/" +
                "ss012eff897271925_11c60l1m1.img.jpeg",
            photo.imageUrl,
        )
        assertEquals("SSI", photo.camera?.name)
        assertEquals("Surface Stereo Imager", photo.camera?.fullName)
        assertEquals("Surface Stereo Imager", photo.name)
        assertEquals("672 nm filter, left eye", photo.description)
        assertEquals("NASA/JPL-Caltech/University of Arizona/Texas A&M University", photo.credit)
    }

    @Test
    fun `an arm camera row has no filter and its own volume and credit`() {
        val parsed = PhoenixCatalogParser.parse(catalogue("RS001EFF896305363_10D31MBM1|1|2008-05-26T21:41:37Z|"))

        val photo = parsed.getValue(1L).single()
        assertEquals("RAC", photo.camera?.name)
        assertEquals("Robotic Arm Camera", photo.camera?.fullName)
        assertTrue(photo.imageUrl.contains("/phxrac_0xxx/extras/browse/sol001/rs001eff896305363_10d31mbm1.img.jpeg"))
        assertNull(photo.description)
        assertEquals("NASA/JPL-Caltech/University of Arizona/Max Planck Institute", photo.credit)
    }

    @Test
    fun `the right eye is named`() {
        val parsed = PhoenixCatalogParser.parse(catalogue("SS000EDN896227848_10C70R1M1|0|2008-05-26T00:09:42Z|672"))

        assertEquals("672 nm filter, right eye", parsed.getValue(0L).single().description)
    }

    @Test
    fun `photos group by sol and keep the order they were taken in`() {
        val parsed = PhoenixCatalogParser.parse(
            catalogue(
                "SS000EDN896227848_10C70R1M1|0|2008-05-26T00:09:42Z|672",
                "SS000EDN896227921_10C70R1M1|0|2008-05-26T00:10:55Z|672",
                "RS001EFF896305363_10D31MBM1|1|2008-05-26T21:41:37Z|",
            )
        )

        assertEquals(setOf(0L, 1L), parsed.keys)
        assertEquals(listOf(0, 1), parsed.getValue(0L).map { it.order })
        assertEquals("SS000EDN896227921_10C70R1M1", parsed.getValue(0L)[1].id)
    }

    @Test
    fun `an unknown header or instrument fails loudly`() {
        assertFailsWith<IllegalArgumentException> {
            PhoenixCatalogParser.parse("phoenix-catalog v2 rows=0\n")
        }
        assertFailsWith<IllegalStateException> {
            PhoenixCatalogParser.parse(catalogue("OS004EFF896565495_11040MBM1|4|2008-05-30T03:07:13Z|"))
        }
    }
}
