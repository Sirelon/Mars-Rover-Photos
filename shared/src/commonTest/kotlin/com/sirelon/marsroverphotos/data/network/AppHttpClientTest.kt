package com.sirelon.marsroverphotos.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AppHttpClientTest {

    private fun client(
        limiter: HostConcurrencyLimiter = HostConcurrencyLimiter(4),
        expectSuccess: Boolean = false,
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) = HttpClient(MockEngine(handler)) {
        this.expectSuccess = expectSuccess
        installHostRequestLimits(limiter = limiter, retryDelay = {})
    }

    /** Answers with [statuses] in order, then 200; counts the calls. */
    private class Script(vararg val statuses: HttpStatusCode) {
        var calls = 0
        fun next(): HttpStatusCode = statuses.getOrElse(calls++) { HttpStatusCode.OK }
    }

    @Test
    fun capsConcurrentRequestsPerHost() = runTest {
        val lock = Mutex()
        val inFlight = mutableMapOf<String, Int>()
        val peak = mutableMapOf<String, Int>()
        val client = client(limiter = HostConcurrencyLimiter(2)) { request ->
            val host = request.url.host
            lock.withLock {
                val now = (inFlight[host] ?: 0) + 1
                inFlight[host] = now
                peak[host] = maxOf(peak[host] ?: 0, now)
            }
            delay(50)
            lock.withLock { inFlight[host] = inFlight.getValue(host) - 1 }
            respond("")
        }

        withContext(Dispatchers.Default) {
            coroutineScope {
                (1..6).flatMap { i ->
                    listOf(
                        async { client.get("https://a.example/$i") },
                        async { client.get("https://b.example/$i") },
                    )
                }.awaitAll()
            }
        }

        assertEquals(mapOf("a.example" to 2, "b.example" to 2), peak)
    }

    @Test
    fun retriesTooManyRequestsUntilSuccess() = runTest {
        val script = Script(HttpStatusCode.TooManyRequests, HttpStatusCode.TooManyRequests)
        val client = client { respond("", script.next()) }

        val response: HttpResponse = client.get("https://planetarydata.example/img")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(3, script.calls)
    }

    @Test
    fun givesUpAfterTwoRetries() = runTest {
        val script = Script(*Array(10) { HttpStatusCode.GatewayTimeout })
        val client = client { respond("", script.next()) }

        val response = client.get("https://planetarydata.example/img")

        assertEquals(HttpStatusCode.GatewayTimeout, response.status)
        assertEquals(3, script.calls)
    }

    @Test
    fun doesNotRetryClientErrors() = runTest {
        val script = Script(HttpStatusCode.NotFound)
        val client = client { respond("", script.next()) }

        val response = client.get("https://images-assets.example/img")

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals(1, script.calls)
    }

    @Test
    fun doesNotRetryPost() = runTest {
        val script = Script(HttpStatusCode.ServiceUnavailable)
        val client = client { respond("", script.next()) }

        val response = client.post("https://api.example/write")

        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        assertEquals(1, script.calls)
    }

    /** RestApi runs with expectSuccess = true; the retry must happen before validation throws. */
    @Test
    fun retriesBeforeValidationWhenExpectSuccess() = runTest {
        val script = Script(HttpStatusCode.ServiceUnavailable)
        val client = client(expectSuccess = true) { respond("", script.next()) }

        val response = client.get("https://mars.example/feed")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(2, script.calls)
    }

    @Test
    fun throwsWhenRetriesRunOutAndExpectSuccess() = runTest {
        val script = Script(*Array(10) { HttpStatusCode.ServiceUnavailable })
        val client = client(expectSuccess = true) { respond("", script.next()) }

        assertFailsWith<ServerResponseException> { client.get("https://mars.example/feed") }
        assertEquals(3, script.calls)
    }
}
