package com.sirelon.marsroverphotos.data.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RequestPhaseTimingsTest {

    private fun ms(value: Long) = value * 1_000_000L

    @Test
    fun freshConnection_splitsIntoPhases() {
        val timings = RequestPhaseTimings().apply {
            callStart = ms(0)
            dnsStart = ms(1); dnsEnd = ms(11)
            connectStart = ms(12); secureConnectStart = ms(40); secureConnectEnd = ms(90); connectEnd = ms(91)
            connectionAcquired = ms(92)
            requestHeadersStart = ms(93); responseHeadersStart = ms(14_093)
            callEnd = ms(14_500)
            connectAttempts = 1
            ipv6 = true
        }

        val metrics = timings.perfMetrics()

        assertEquals(10, metrics["dns_ms"])
        assertEquals(28, metrics["connect_ms"])
        assertEquals(50, metrics["tls_ms"])
        assertEquals(92, metrics["acquire_ms"])
        assertEquals(14_000, metrics["ttfb_ms"])
        assertEquals(407, metrics["body_ms"])
        assertEquals(14_500, metrics["total_ms"])
        assertEquals(0, metrics["reused"])
        assertEquals(1, metrics["ipv6"])
        assertTrue(timings.isSlow)
    }

    @Test
    fun pooledConnection_hasNoDnsConnectOrTls() {
        val timings = RequestPhaseTimings().apply {
            callStart = ms(0); connectionAcquired = ms(1)
            requestHeadersStart = ms(1); responseHeadersStart = ms(300); callEnd = ms(320)
        }

        val metrics = timings.perfMetrics()

        assertFalse("dns_ms" in metrics || "connect_ms" in metrics || "tls_ms" in metrics)
        assertEquals(1, metrics["reused"])
        assertEquals(299, metrics["ttfb_ms"])
    }

    @Test
    fun reportsEverySlowCallButOnlySamplesFastOnes() {
        val slow = RequestPhaseTimings().apply { callStart = 0; callEnd = ms(RequestPhaseTimings.SLOW_CALL_MS) }
        val fast = RequestPhaseTimings().apply { callStart = 0; callEnd = ms(200) }

        assertTrue(slow.shouldReport(randomUnit = 0.99))
        assertFalse(fast.shouldReport(randomUnit = 0.99))
        assertTrue(fast.shouldReport(randomUnit = 0.01))
    }

    @Test
    fun outcomeAndContentKind() {
        val timings = RequestPhaseTimings()
        assertEquals("unknown", timings.outcome)

        timings.responseCode = 429
        timings.contentType = "image/jpeg"
        assertEquals("http_4xx", timings.outcome)
        assertEquals("image", timings.contentKind)

        timings.contentType = "application/json; charset=utf-8"
        timings.failure = "canceled"
        assertEquals("canceled", timings.outcome)
        assertEquals("json", timings.contentKind)
    }
}
