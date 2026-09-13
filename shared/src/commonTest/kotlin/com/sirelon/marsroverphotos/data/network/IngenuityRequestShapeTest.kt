package com.sirelon.marsroverphotos.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Guards the shape of the outgoing Mars 2020 raw-image requests.
 *
 * The feed does not resolve a duplicated query parameter as "last wins" — it discards both and
 * falls back to its own default. A second `category` in the URL therefore answers an Ingenuity
 * request with Perseverance photos: no error, no failing assertion anywhere else in the suite,
 * and the wrong mission's images cached under Ingenuity's rover id. Same trap for `order` on the
 * nearest-sol lookup, which needs ascending order and would silently get the newest sol instead.
 *
 * These tests exist because that failure is invisible to every other test we have.
 */
class IngenuityRequestShapeTest {

    private val emptyFeed = """{"images":[],"total_images":0}"""

    /** Mirrors the production configuration in [RestApi], so decoding behaves the same here. */
    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private fun apiRecording(capture: MutableList<Url>, body: String = emptyFeed): NasaApi {
        val engine = MockEngine { request ->
            capture += request.url
            respond(
                content = body,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        return NasaApi(client)
    }

    @Test
    fun ingenuityFeed_sendsCategoryExactlyOnce() = runTest {
        val urls = mutableListOf<Url>()

        apiRecording(urls).getPerseveranceRawImages(sol = "1069:sol:in", category = "ingenuity")

        val params = urls.single().parameters
        assertEquals(listOf("ingenuity"), params.getAll("category"))
    }

    @Test
    fun perseveranceFeed_stillDefaultsToMars2020Once() = runTest {
        val urls = mutableListOf<Url>()

        apiRecording(urls).getPerseveranceRawImages(sol = "1000:sol:in")

        val params = urls.single().parameters
        assertEquals(listOf("mars2020"), params.getAll("category"))
        assertEquals(listOf("sol desc"), params.getAll("order"))
    }

    @Test
    fun nearestSolLookup_asksForAscendingOrderExactlyOnce() = runTest {
        val urls = mutableListOf<Url>()

        apiRecording(urls).getIngenuityNearestSolAtOrAfter(777L)

        val params = urls.single().parameters
        // Ascending, or the feed answers with the last sol of the mission instead of the nearest.
        assertEquals(listOf("sol asc"), params.getAll("order"))
        assertEquals(listOf("ingenuity"), params.getAll("category"))
        assertEquals("777:sol:gte", params["condition_2"])
    }

    @Test
    fun nearestSolLookup_returnsSolFromResponse() = runTest {
        // Shaped like a real Ingenuity record, down to the "UNK" filter name the feed always
        // reports for both of the helicopter's cameras.
        val body = """
            {"images":[{
              "imageid":"HNM_852_0000000000_000ECM_N0000000HELI00000_0000A0J",
              "sol":852,
              "title":"Mars Helicopter Sol 852: Navigation Camera",
              "camera":{"instrument":"HELI_NAV","camera_model_type":"CAHVORE","filter_name":"UNK"},
              "image_files":{
                "medium":"https://example.test/852_800.jpg",
                "small":"https://example.test/852_320.jpg",
                "large":"https://example.test/852_1200.jpg",
                "full_res":"https://example.test/852.png"
              },
              "date_taken_utc":"2023-05-01T00:00:00"
            }],"total_images":1}
        """.trimIndent()

        val sol = apiRecording(mutableListOf(), body).getIngenuityNearestSolAtOrAfter(777L)

        assertEquals(852L, sol)
    }

    @Test
    fun nearestSolLookup_returnsNullWhenNoSolRemains() = runTest {
        val sol = apiRecording(mutableListOf()).getIngenuityNearestSolAtOrAfter(1070L)

        assertNull(sol)
    }
}
