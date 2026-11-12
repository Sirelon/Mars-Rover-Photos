package com.sirelon.marsroverphotos.data.network

/**
 * Where the time of one HTTP call went: DNS, TCP connect, TLS, time to first byte (SIR-167).
 *
 * The engine fills the timestamps in monotonic nanoseconds. A phase that did not happen stays
 * null and is left out of [perfMetrics]: a pooled connection skips DNS, connect and TLS. When a
 * connect races several addresses, the first start and the last end count.
 *
 * Only Android reports these (`PhaseTimingEventListener`, Firebase Performance trace
 * [TRACE_NAME]). The question is why Android stalls about 6× more often than iOS on the same
 * hosts; Darwin exposes the same phases only through a custom URLSession delegate.
 */
class RequestPhaseTimings {
    var host: String = ""
    var callStart: Long? = null
    var dnsStart: Long? = null
    var dnsEnd: Long? = null
    var connectStart: Long? = null
    var secureConnectStart: Long? = null
    var secureConnectEnd: Long? = null
    var connectEnd: Long? = null
    var connectionAcquired: Long? = null
    var requestHeadersStart: Long? = null
    var responseHeadersStart: Long? = null
    var callEnd: Long? = null

    var connectAttempts: Int = 0
    var ipv6: Boolean = false
    var protocol: String? = null
    var responseCode: Int? = null
    var contentType: String? = null

    /** Exception class name, or `canceled`, when the call failed. */
    var failure: String? = null

    val totalMs: Long? get() = spanMs(callStart, callEnd)

    /** Durations in milliseconds plus the connection counters, keyed by Firebase metric name. */
    fun perfMetrics(): Map<String, Long> = buildMap {
        spanMs(callStart, callEnd)?.let { put("total_ms", it) }
        spanMs(callStart, connectionAcquired)?.let { put("acquire_ms", it) }
        spanMs(dnsStart, dnsEnd)?.let { put("dns_ms", it) }
        spanMs(connectStart, secureConnectStart ?: connectEnd)?.let { put("connect_ms", it) }
        spanMs(secureConnectStart, secureConnectEnd)?.let { put("tls_ms", it) }
        spanMs(requestHeadersStart, responseHeadersStart)?.let { put("ttfb_ms", it) }
        spanMs(responseHeadersStart, callEnd)?.let { put("body_ms", it) }
        put("connect_attempts", connectAttempts.toLong())
        put("reused", if (connectAttempts == 0 && connectionAcquired != null) 1 else 0)
        put("ipv6", if (ipv6) 1 else 0)
    }

    /** `ok`, `http_4xx`, `http_5xx`, `canceled`, or the exception class name. */
    val outcome: String
        get() = failure ?: when (val code = responseCode) {
            null -> "unknown"
            in 200..399 -> "ok"
            else -> "http_${code / 100}xx"
        }

    val contentKind: String
        get() = when {
            contentType?.startsWith("image/") == true -> "image"
            contentType?.contains("json") == true -> "json"
            else -> "other"
        }

    val isSlow: Boolean get() = (totalMs ?: 0) >= SLOW_CALL_MS

    /** Every slow call, plus a baseline sample of the rest to compare against. */
    fun shouldReport(randomUnit: Double): Boolean = isSlow || randomUnit < BASELINE_SAMPLE_RATE

    private fun spanMs(start: Long?, end: Long?): Long? =
        if (start != null && end != null && end >= start) (end - start) / NANOS_PER_MS else null

    companion object {
        const val TRACE_NAME = "http_phases"
        const val SLOW_CALL_MS = 5_000L
        const val BASELINE_SAMPLE_RATE = 0.05
        private const val NANOS_PER_MS = 1_000_000L
    }
}
