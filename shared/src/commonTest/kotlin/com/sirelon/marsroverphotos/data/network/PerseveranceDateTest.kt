package com.sirelon.marsroverphotos.data.network

import com.sirelon.marsroverphotos.data.network.models.PerseverancePhotoItemResponse
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class PerseveranceDateTest {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    private fun item(dates: String) = json.decodeFromString<PerseverancePhotoItemResponse>(
        """{"imageid":"ZL0_2024","sol":2024,"title":null,"caption":null,"credit":null,"image_files":null,"camera":null,$dates}"""
    )

    // The live feed lists date_taken_utc before date_received; the photo's date is the first.
    @Test
    fun usesTheDateTakenWhenTheFeedSendsBoth() {
        val response = item(""""date_taken_utc":"2026-10-30T09:21:53.576","date_received":"2026-11-04T03:39:55Z"""")

        assertEquals("2026-10-30T09:21:53.576", response.earthDate)
    }

    @Test
    fun fallsBackToTheReceiveDate() {
        assertEquals("2026-11-04T03:39:55Z", item(""""date_received":"2026-11-04T03:39:55Z"""").earthDate)
    }
}
