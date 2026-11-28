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
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Guards the by-date raw-feed requests behind "A day on Mars". Both feeds take a range as
 * `<value>:date_taken:<op>` in the from/to condition slots (verified live 2026-11-28), and the
 * Mars 2020 feed discards a duplicated parameter instead of overriding it — see
 * [IngenuityRequestShapeTest].
 */
class PhotosTakenOnRequestShapeTest {

    private fun apiRecording(capture: MutableList<Url>, body: String): NasaApi {
        val engine = MockEngine { request ->
            capture += request.url
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return NasaApi(HttpClient(engine) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } })
    }

    @Test
    fun mars2020Feed_asksForOneUtcDateOfFullFrames_eachParameterOnce() = runTest {
        val urls = mutableListOf<Url>()

        apiRecording(urls, """{"images":[],"total_images":1055871,"total_results":0}""")
            .getMars2020RawImagesTakenOn("ingenuity", LocalDate(2021, 4, 19))

        val params = urls.single().parameters
        assertEquals(listOf("ingenuity"), params.getAll("category"))
        assertEquals(listOf("2021-04-19:date_taken:gte"), params.getAll("condition_2"))
        assertEquals(listOf("2021-04-20:date_taken:lt"), params.getAll("condition_3"))
        assertEquals(listOf("sample_type::full,"), params.getAll("extended"))
    }

    @Test
    fun rawImageItemsFeed_asksForOneUtcDateOfFullFrames() = runTest {
        val urls = mutableListOf<Url>()

        apiRecording(urls, """{"items":[],"total":0}""").getRawImages(
            "msl",
            from = LocalDate(2023, 12, 31).takenFromCondition(),
            to = LocalDate(2023, 12, 31).takenBeforeCondition(),
            fullFramesOnly = true,
        )

        val params = urls.single().parameters
        assertEquals(listOf("2023-12-31:date_taken:gte"), params.getAll("condition_2"))
        // The next day crosses the year: plain string arithmetic would get this wrong.
        assertEquals(listOf("2024-01-01:date_taken:lt"), params.getAll("condition_3"))
        assertEquals(listOf("thumbnail::sample_type::noteq"), params.getAll("extended"))
    }
}
