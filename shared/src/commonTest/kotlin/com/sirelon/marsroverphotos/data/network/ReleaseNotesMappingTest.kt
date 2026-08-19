package com.sirelon.marsroverphotos.data.network

import com.sirelon.marsroverphotos.data.network.models.ReleaseChangeDto
import com.sirelon.marsroverphotos.data.network.models.ReleaseDto
import com.sirelon.marsroverphotos.data.network.models.toDomain
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbol
import com.sirelon.marsroverphotos.presentation.ui.materialSymbolOrDefault
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Release-note documents are hand-authored in Firestore, so every mapping decision here is about a
 * document arriving in a shape this build did not write.
 */
class ReleaseNotesMappingTest {

    private fun change(
        id: String = "some_change",
        icon: String = "rocket_launch",
        title: String = "Mission Info",
        summary: String = "A richer look",
        detail: String? = "The long version.",
        imageUrl: String? = "https://example.com/a.jpg",
    ) = ReleaseChangeDto(id, icon, title, summary, detail, imageUrl)

    @Test
    fun mapsAFullDocument() {
        val release = ReleaseDto(
            version = "5.0.0",
            date = "2026-06-29",
            changes = listOf(change()),
        ).toDomain()

        assertEquals("5.0.0", release?.version)
        assertEquals(LocalDate(2026, 6, 29), release?.date)
        assertEquals(1, release?.changes?.size)
        val mapped = release?.changes?.single()
        assertEquals("some_change", mapped?.id)
        assertEquals("rocket_launch", mapped?.icon)
        assertEquals("Mission Info", mapped?.title)
        assertEquals("A richer look", mapped?.summary)
        assertEquals("The long version.", mapped?.detail)
        assertEquals("https://example.com/a.jpg", mapped?.imageUrl)
    }

    @Test
    fun dropsDocumentWithoutAVersion() {
        assertNull(ReleaseDto(version = " ", date = "2026-06-29", changes = listOf(change())).toDomain())
    }

    @Test
    fun dropsDocumentWithUnparseableDate() {
        // Anything but ISO-8601: a Firestore Timestamp written by hand, a locale format, or nothing.
        for (date in listOf("29/06/2026", "June 29, 2026", "", "2026-13-45")) {
            assertNull(
                ReleaseDto(version = "5.0.0", date = date, changes = listOf(change())).toDomain(),
                "expected \"$date\" to be rejected",
            )
        }
    }

    @Test
    fun dropsDocumentWithNoRenderableChanges() {
        assertNull(ReleaseDto(version = "5.0.0", date = "2026-06-29").toDomain())
        assertNull(
            ReleaseDto(
                version = "5.0.0",
                date = "2026-06-29",
                changes = listOf(change(title = " ")),
            ).toDomain(),
        )
    }

    @Test
    fun keepsGoodChangesAlongsideUntitledOnes() {
        val release = ReleaseDto(
            version = "5.0.0",
            date = "2026-06-29",
            changes = listOf(change(id = "keep"), change(id = "drop", title = ""), change(id = "keep2")),
        ).toDomain()

        assertEquals(listOf("keep", "keep2"), release?.changes?.map { it.id })
    }

    @Test
    fun blankOptionalStringsBecomeNull() {
        val mapped = change(detail = "  ", imageUrl = "").toDomain()
        assertNull(mapped?.detail)
        assertNull(mapped?.imageUrl)
    }

    @Test
    fun changeWithoutIdFallsBackToItsTitle() {
        // The id only has to be stable within a release; a document authored without one still works.
        assertEquals("Mission Info", change(id = "").toDomain()?.id)
    }

    @Test
    fun iconNameResolvesToItsSymbol() {
        assertEquals(MaterialSymbol.Rocket, materialSymbolOrDefault("rocket_launch"))
        assertEquals(MaterialSymbol.BugReport, materialSymbolOrDefault("bug_report"))
    }

    @Test
    fun iconNameIsCaseInsensitive() {
        assertEquals(MaterialSymbol.Rocket, materialSymbolOrDefault("Rocket_Launch"))
    }

    @Test
    fun unknownIconNameFallsBackToTheDefault() {
        // The whole point: a note naming a symbol this build lacks must still render something.
        for (name in listOf("rocket_lanuch", "not_a_real_icon", "", "  ", null)) {
            assertEquals(
                MaterialSymbol.Star,
                materialSymbolOrDefault(name),
                "expected \"$name\" to fall back",
            )
        }
        assertEquals(MaterialSymbol.Info, materialSymbolOrDefault("nope", default = MaterialSymbol.Info))
    }
}
