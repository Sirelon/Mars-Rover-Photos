package com.sirelon.marsroverphotos.data.viking

import com.sirelon.marsroverphotos.domain.models.VIKING_1_ID
import com.sirelon.marsroverphotos.domain.models.VIKING_2_ID
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The catalogue text these assert against is generated and URL-verified by
 * `scripts/generate-viking-catalog.mjs`; the fixtures here are real rows lifted from its output.
 */
class VikingCatalogParserTest {

    private fun catalogue(vararg rows: String, notes: List<String> = listOf("First Lander 1 Image")) =
        (listOf("viking-catalog v1 lander=1 volume=vl_0001 notes=${notes.size} rows=${rows.size}") +
            notes + rows.toList()).joinToString("\n") + "\n"

    @Test
    fun `parses a row into a sol-keyed photo`() {
        val parsed = VikingCatalogParser.parse(
            catalogue("A0XX/12A001.BB1|0|1976-07-20|0"),
            VIKING_1_ID,
        )

        val photo = parsed.getValue(0L).single()
        assertEquals("12A001-BB1", photo.id)
        assertEquals(0L, photo.sol)
        assertEquals("1976-07-20", photo.earthDate)
        assertEquals("First Lander 1 Image", photo.name)
        assertEquals(VIKING_1_ID, photo.roverId)
        assertEquals("NASA/JPL", photo.credit)
        assertEquals(0, photo.order)
    }

    @Test
    fun `derives the browse url by lowercasing and abbreviating the filter extension`() {
        val parsed = VikingCatalogParser.parse(
            catalogue("A0XX/12A001.BB1|0|1976-07-20|0"),
            VIKING_1_ID,
        )

        assertEquals(
            "https://planetarydata.jpl.nasa.gov/img/data/vl1_vl2-m-lcs-2-edr-v1.0" +
                "/vl_0001/extras/browse/a0xx/12a001b1.jpeg",
            parsed.getValue(0L).single().imageUrl,
        )
    }

    @Test
    fun `derives urls for every filter family in the archive`() {
        // 1st and 3rd characters of the extension, lowercased: BB2 -> b2, SUR -> sr, N07 -> n7.
        val rows = listOf(
            "A0XX/12A003.BB2|1|1976-07-21|0" to "a0xx/12a003b2.jpeg",
            "A0XX/12A002.SUR|0|1976-07-20|0" to "a0xx/12a002sr.jpeg",
            "A0XX/11A019.N07|3|1976-07-23|0" to "a0xx/11a019n7.jpeg",
            "A0XX/11A021.RED|3|1976-07-23|0" to "a0xx/11a021rd.jpeg",
            "A0XX/11A021.GRN|3|1976-07-23|0" to "a0xx/11a021gn.jpeg",
            "A0XX/11A021.BLU|3|1976-07-23|0" to "a0xx/11a021bu.jpeg",
            "J1XX/12J194.IR1|2238|1982-11-05|0" to "j1xx/12j194i1.jpeg",
        )
        val parsed = VikingCatalogParser.parse(catalogue(*rows.map { it.first }.toTypedArray()), VIKING_1_ID)

        val urls = parsed.values.flatten().map { it.imageUrl.substringAfter("/extras/browse/") }.toSet()
        assertEquals(rows.map { it.second }.toSet(), urls)
    }

    @Test
    fun `reads the camera number from the product stem`() {
        val parsed = VikingCatalogParser.parse(
            catalogue(
                "A0XX/12A001.BB1|0|1976-07-20|0",
                "A0XX/11A017.BB3|2|1976-07-22|0",
            ),
            VIKING_1_ID,
        )

        val cameras = parsed.values.flatten().map { it.camera }
        assertContentEquals(listOf("VLC2", "VLC1"), cameras.map { it?.name })
        assertContentEquals(listOf(2, 1), cameras.map { it?.id })
        assertEquals("Viking Lander Camera 2", cameras.first()?.fullName)
    }

    @Test
    fun `labels the filter wheel position as the photo description`() {
        val parsed = VikingCatalogParser.parse(
            catalogue(
                "A0XX/12A001.BB1|0|1976-07-20|0",
                "A0XX/12A002.SUR|0|1976-07-20|0",
                "A0XX/11A021.RED|0|1976-07-20|0",
                "A0XX/11A019.N07|0|1976-07-20|0",
                "A0XX/12A100.IR2|0|1976-07-20|0",
            ),
            VIKING_1_ID,
        )

        assertContentEquals(
            listOf("Broadband filter 1", "Survey scan", "Red filter", "Calibration frame", "Infrared filter 2"),
            parsed.getValue(0L).map { it.description },
        )
    }

    @Test
    fun `groups photos by sol and orders them within the sol`() {
        val parsed = VikingCatalogParser.parse(
            catalogue(
                "A0XX/12A001.BB1|0|1976-07-20|0",
                "A0XX/12A002.SUR|0|1976-07-20|0",
                "A0XX/12A003.BB2|1|1976-07-21|0",
            ),
            VIKING_1_ID,
        )

        assertEquals(setOf(0L, 1L), parsed.keys)
        assertContentEquals(listOf(0, 1), parsed.getValue(0L).map { it.order })
        assertEquals(0, parsed.getValue(1L).single().order)
    }

    @Test
    fun `a sol with no photos is simply absent`() {
        val parsed = VikingCatalogParser.parse(catalogue("A0XX/12A001.BB1|0|1976-07-20|0"), VIKING_1_ID)

        // SolPagingSource reads this as "keep scanning", so an absent sol must not throw.
        assertNull(parsed[500L])
        assertTrue(parsed[500L].orEmpty().isEmpty())
    }

    @Test
    fun `dereferences captions by index including the last one`() {
        val notes = listOf("First Lander 1 Image", "Monitor Sample Site", "Survey Panorama")
        val parsed = VikingCatalogParser.parse(
            catalogue(
                "A0XX/12A001.BB1|0|1976-07-20|0",
                "A0XX/12A002.SUR|1|1976-07-21|2",
                notes = notes,
            ),
            VIKING_1_ID,
        )

        assertEquals("First Lander 1 Image", parsed.getValue(0L).single().name)
        assertEquals("Survey Panorama", parsed.getValue(1L).single().name)
    }

    @Test
    fun `rejects an unrecognised header rather than mis-parsing`() {
        assertFailsWith<IllegalArgumentException> {
            VikingCatalogParser.parse("viking-catalog v2 volume=vl_0001 notes=0 rows=0\n", VIKING_1_ID)
        }
    }

    @Test
    fun `maps rover ids to their catalogue resource`() {
        assertEquals("viking1_catalog.txt", VikingCatalogParser.resourceFor(VIKING_1_ID))
        assertEquals("viking2_catalog.txt", VikingCatalogParser.resourceFor(VIKING_2_ID))
        assertFailsWith<IllegalStateException> { VikingCatalogParser.resourceFor(5L) }
    }
}
