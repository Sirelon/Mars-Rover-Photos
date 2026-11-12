package com.sirelon.marsroverphotos.platform

import com.google.firebase.perf.FirebasePerformance
import com.sirelon.marsroverphotos.data.network.RequestPhaseTimings
import com.sirelon.marsroverphotos.utils.Logger
import okhttp3.Call
import okhttp3.Connection
import okhttp3.EventListener
import okhttp3.Handshake
import okhttp3.Protocol
import okhttp3.Response
import java.io.IOException
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import kotlin.random.Random

/**
 * Times each OkHttp call phase by phase and hands the result to [report] when the call ends (SIR-167).
 *
 * One instance per call. Connect attempts can run on several threads at once (fast fallback), so
 * every write is synchronized on the timings.
 */
internal class PhaseTimingEventListener(
    private val report: (RequestPhaseTimings) -> Unit,
) : EventListener() {

    private val timings = RequestPhaseTimings()

    private inline fun record(block: RequestPhaseTimings.(now: Long) -> Unit) {
        val now = System.nanoTime()
        synchronized(timings) { timings.block(now) }
    }

    override fun callStart(call: Call) = record {
        callStart = it
        host = call.request().url.host
    }

    override fun dnsStart(call: Call, domainName: String) = record { if (dnsStart == null) dnsStart = it }

    override fun dnsEnd(call: Call, domainName: String, inetAddressList: List<InetAddress>) =
        record { dnsEnd = it }

    override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) = record {
        if (connectStart == null) connectStart = it
        connectAttempts++
    }

    override fun secureConnectStart(call: Call) = record { if (secureConnectStart == null) secureConnectStart = it }

    override fun secureConnectEnd(call: Call, handshake: Handshake?) = record { secureConnectEnd = it }

    override fun connectEnd(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy, protocol: Protocol?) =
        record { connectEnd = it }

    override fun connectionAcquired(call: Call, connection: Connection) = record {
        connectionAcquired = it
        protocol = connection.protocol().toString()
        ipv6 = connection.route().socketAddress.address is Inet6Address
    }

    override fun requestHeadersStart(call: Call) = record { if (requestHeadersStart == null) requestHeadersStart = it }

    override fun responseHeadersStart(call: Call) = record { if (responseHeadersStart == null) responseHeadersStart = it }

    override fun responseHeadersEnd(call: Call, response: Response) = record {
        responseCode = response.code
        contentType = response.header("Content-Type")
    }

    override fun callEnd(call: Call) {
        record { callEnd = it }
        report(timings)
    }

    override fun callFailed(call: Call, ioe: IOException) {
        record {
            callEnd = it
            failure = if (call.isCanceled()) "canceled" else ioe::class.simpleName ?: "IOException"
        }
        report(timings)
    }
}

/**
 * Sends a sampled [RequestPhaseTimings] as a Firebase Performance custom trace.
 *
 * The trace is started and stopped at once, so its own duration means nothing: read `total_ms` and
 * the phase metrics. Firebase allows 5 attributes per trace, so the yes/no facts go in as 0/1
 * metrics. Never throws: a reporting failure must not fail the request.
 */
internal fun reportToFirebasePerformance(timings: RequestPhaseTimings) {
    if (!timings.shouldReport(Random.nextDouble())) return
    runCatching {
        FirebasePerformance.getInstance().newTrace(RequestPhaseTimings.TRACE_NAME).apply {
            putAttribute("host", timings.host.take(MAX_ATTRIBUTE_LENGTH))
            putAttribute("content", timings.contentKind)
            putAttribute("outcome", timings.outcome.take(MAX_ATTRIBUTE_LENGTH))
            putAttribute("protocol", timings.protocol ?: "none")
            putAttribute("slow", timings.isSlow.toString())
            timings.perfMetrics().forEach { (name, value) -> putMetric(name, value) }
            start()
            stop()
        }
    }.onFailure { Logger.e("PhaseTiming", it) { "Could not report HTTP phase timings" } }
}

private const val MAX_ATTRIBUTE_LENGTH = 100
