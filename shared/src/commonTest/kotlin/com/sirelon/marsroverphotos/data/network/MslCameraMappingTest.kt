package com.sirelon.marsroverphotos.data.network

import com.sirelon.marsroverphotos.data.network.models.PhotosResponse
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies the MSL instrument → RoverCamera mapping used by mapToUiMsl().
 *
 * The leading-token rule must produce camera names that match Curiosity's CameraSpec.name
 * values in RoverMissionData (FHAZ, RHAZ, MAST, CHEMCAM, MAHLI, MARDI, NAVCAM) so that
 * SolPagingSource.filterByCameras works without modification.
 */
class MslCameraMappingTest {

    @Test
    fun instrumentToken_multiSegment_returnsLeadingToken() {
        assertEquals("MAST", "MAST_RIGHT".mslInstrumentToken())
        assertEquals("MAST", "MAST_LEFT".mslInstrumentToken())
        assertEquals("RHAZ", "RHAZ_LEFT_A".mslInstrumentToken())
        assertEquals("RHAZ", "RHAZ_RIGHT_B".mslInstrumentToken())
        assertEquals("FHAZ", "FHAZ_LEFT_B".mslInstrumentToken())
        assertEquals("CHEMCAM", "CHEMCAM_RMI".mslInstrumentToken())
    }

    @Test
    fun instrumentToken_noUnderscore_returnsWholeString() {
        assertEquals("FHAZ", "FHAZ".mslInstrumentToken())
        assertEquals("MAHLI", "MAHLI".mslInstrumentToken())
        assertEquals("MARDI", "MARDI".mslInstrumentToken())
        assertEquals("CHEMCAM", "CHEMCAM".mslInstrumentToken())
    }

    // Regression: NAV_* instruments must resolve to "NAVCAM" not "NAV".
    // The live MSL feed reports navigation cameras as NAV_LEFT_A, NAV_LEFT_B, NAV_RIGHT_A,
    // NAV_RIGHT_B — token "NAV" — but Curiosity's CameraSpec.name (and the filter chip) is "NAVCAM".
    @Test
    fun instrumentToken_navPrefix_resolvesToNavcam() {
        assertEquals("NAVCAM", "NAV_LEFT_A".mslInstrumentToken())
        assertEquals("NAVCAM", "NAV_LEFT_B".mslInstrumentToken())
        assertEquals("NAVCAM", "NAV_RIGHT_A".mslInstrumentToken())
        assertEquals("NAVCAM", "NAV_RIGHT_B".mslInstrumentToken())
    }

    @Test
    fun mapToUiMsl_setsLeadingTokenAsCameraName() {
        val photos = listOf(
            fakeMarsPhoto(id = "a", instrument = "MAST_RIGHT"),
            fakeMarsPhoto(id = "b", instrument = "NAV_LEFT_A"),  // alias: NAV → NAVCAM
            fakeMarsPhoto(id = "c", instrument = "FHAZ"),
            fakeMarsPhoto(id = "d", instrument = null),
        )

        val images = photos.mapToUiMsl()

        assertEquals("MAST", images[0].camera?.name)
        assertEquals("MAST_RIGHT", images[0].camera?.fullName)
        assertEquals("NAVCAM", images[1].camera?.name)   // alias applied
        assertEquals("NAV_LEFT_A", images[1].camera?.fullName)  // raw instrument preserved
        assertEquals("FHAZ", images[2].camera?.name)
        assertEquals(null, images[3].camera)
    }

    @Test
    fun mapToUiMsl_cameraNameMatchesCuriosityFilter() {
        // Simulate the filterByCameras name-equality check: cam.name.equals(filter, ignoreCase = true)
        val photos = listOf(fakeMarsPhoto(id = "x", instrument = "MAST_RIGHT"))
        val images = photos.mapToUiMsl()
        val cam = images.first().camera!!

        assertEquals(true, cam.name.equals("MAST", ignoreCase = true))
        assertEquals(false, cam.name.equals("NAVCAM", ignoreCase = true))
    }

    @Test
    fun rawFeed_numericId_decodesAsString() {
        val json = Json { ignoreUnknownKeys = true }
        val response = json.decodeFromString<PhotosResponse>(
            """
            {
              "items": [{
                "id": 1598100,
                "sol": 4917,
                "title": "Sol 4917: Rear Hazard Avoidance Camera",
                "url": "https://mars.nasa.gov/msl-raw-images/example.JPG",
                "created_at": "2026-06-06T17:40:15.792Z",
                "instrument": "RHAZ_RIGHT_B"
              }],
              "total": 1
            }
            """.trimIndent()
        )

        assertEquals("1598100", response.list.single().id)
    }
}

private fun fakeMarsPhoto(
    id: String,
    instrument: String?,
) = com.sirelon.marsroverphotos.domain.models.MarsPhoto(
    id = id,
    sol = 100L,
    name = "test-$id",
    imageUrl = "https://example.test/$id.jpg",
    earthDate = "2024-01-01",
    camera = null,
    instrument = instrument,
)
