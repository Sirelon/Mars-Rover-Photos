package com.sirelon.marsroverphotos.presentation.ui

import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.domain.models.CURIOSITY_ID
import com.sirelon.marsroverphotos.domain.models.OPPORTUNITY_ID
import com.sirelon.marsroverphotos.domain.models.PERSEVERANCE_ID
import com.sirelon.marsroverphotos.domain.models.RoverCamera
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies [photoContentDescription] builds a TalkBack/VoiceOver label from a photo's rover,
 * camera and sol instead of ever surfacing its raw URL (see MarsImage.kt for the format/fallback
 * rules) — Spirit/Opportunity (NASA Image Library) lack a sol and camera, so those parts must be
 * omitted rather than printed as "sol 0" or "null".
 */
class MarsImagePhotoContentDescriptionTest {

    @Test
    fun fullData_joinsRoverCameraAndSol() {
        val image = testImage(
            roverId = PERSEVERANCE_ID,
            sol = 1234,
            camera = RoverCamera(id = 1, name = "MCZ", fullName = "Mastcam-Z"),
            earthDate = "2024-05-01",
        )
        assertEquals("Perseverance, Mastcam-Z, sol 1,234", image.photoContentDescription())
    }

    @Test
    fun missingSol_fallsBackToEarthDate() {
        // Spirit/Opportunity come from the NASA Image Library with no sol (stored as 0) and no
        // camera — see Mappers.kt#toMarsImages.
        val image = testImage(
            roverId = OPPORTUNITY_ID,
            sol = 0,
            camera = null,
            earthDate = "2004-05-12",
        )
        assertEquals("Opportunity, 2004-05-12", image.photoContentDescription())
    }

    @Test
    fun missingCamera_omitsCameraPart() {
        val image = testImage(
            roverId = CURIOSITY_ID,
            sol = 1505,
            camera = null,
            earthDate = "2017-09-18",
        )
        assertEquals("Curiosity, sol 1,505", image.photoContentDescription())
    }

    @Test
    fun sol_isFormattedWithThousandsSeparator() {
        val image = testImage(
            roverId = CURIOSITY_ID,
            sol = 12_345,
            camera = null,
            earthDate = "",
        )
        assertEquals("Curiosity, sol 12,345", image.photoContentDescription())
    }

    @Test
    fun nothingStructured_fallsBackToTitle() {
        // Unrecognized rover id, no sol, no earth date — only a title survives.
        val image = testImage(
            roverId = UNKNOWN_ROVER_ID,
            sol = 0,
            camera = null,
            earthDate = "",
            name = "Rover selfie",
        )
        assertEquals("Rover selfie", image.photoContentDescription())
    }

    @Test
    fun nothingStructuredAndNoTitle_fallsBackToDescription() {
        val image = testImage(
            roverId = UNKNOWN_ROVER_ID,
            sol = 0,
            camera = null,
            earthDate = "",
            name = null,
            description = "A close-up of a wind-carved rock",
        )
        assertEquals("A close-up of a wind-carved rock", image.photoContentDescription())
    }

    @Test
    fun nothingAtAll_fallsBackToLiteral() {
        val image = testImage(
            roverId = UNKNOWN_ROVER_ID,
            sol = 0,
            camera = null,
            earthDate = "",
            name = null,
            description = null,
        )
        assertEquals("Mars photo", image.photoContentDescription())
    }

    @Test
    fun neverReturnsTheImageUrl() {
        val image = testImage(roverId = PERSEVERANCE_ID, sol = 10, camera = null, earthDate = "")
        assertEquals(false, image.photoContentDescription().contains("http"))
    }

    private companion object {
        /** No rover in this app owns this id, so [photoDisplayName] resolution intentionally fails. */
        const val UNKNOWN_ROVER_ID = -1L
    }

    private fun testImage(
        roverId: Long,
        sol: Long,
        camera: RoverCamera?,
        earthDate: String,
        name: String? = "img",
        description: String? = null,
    ): MarsImage = MarsImage(
        id = "test-id",
        order = 0,
        sol = sol,
        name = name,
        imageUrl = "https://example.test/photo.jpg",
        earthDate = earthDate,
        roverId = roverId,
        camera = camera,
        stats = MarsImage.Stats(see = 0, scale = 0, save = 0, share = 0, favorite = 0),
        description = description,
    )
}
