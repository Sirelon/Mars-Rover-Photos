package com.sirelon.marsroverphotos.platform

import com.google.firebase.perf.FirebasePerformance
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.Handshake
import okhttp3.Protocol
import okhttp3.Request
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy

/**
 * Reports where a slow request spent its time (SIR-167): DNS, connect, TLS, or waiting for the
 * server. Calls of 5 s or more go to Firebase Performance as the trace `slow_http_call`.
 * `connect_ms` includes `tls_ms`. A pooled connection skips DNS and connect, so those stay 0.
 */
internal class SlowCallListener : EventListener() {
    private var callStart = 0L
    private var dnsStart = 0L
    private var dnsMs = 0L
    private var connectStart = 0L
    private var connectMs = 0L
    private var tlsStart = 0L
    private var tlsMs = 0L
    private var requestSent = 0L
    private var serverWaitMs = 0L

    override fun callStart(call: Call) { callStart = now() }
    override fun dnsStart(call: Call, domainName: String) { dnsStart = now() }
    override fun dnsEnd(call: Call, domainName: String, inetAddressList: List<InetAddress>) { dnsMs = msSince(dnsStart) }
    override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) { connectStart = now() }
    override fun connectEnd(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy, protocol: Protocol?) { connectMs = msSince(connectStart) }
    override fun secureConnectStart(call: Call) { tlsStart = now() }
    override fun secureConnectEnd(call: Call, handshake: Handshake?) { tlsMs = msSince(tlsStart) }
    override fun requestHeadersEnd(call: Call, request: Request) { requestSent = now() }
    override fun responseHeadersStart(call: Call) { serverWaitMs = msSince(requestSent) }
    override fun callEnd(call: Call) = report(call, outcome = "ok")
    override fun callFailed(call: Call, ioe: IOException) = report(call, outcome = ioe::class.simpleName ?: "error")

    private fun report(call: Call, outcome: String) {
        val totalMs = msSince(callStart)
        if (totalMs < 5_000) return
        runCatching {
            FirebasePerformance.getInstance().newTrace("slow_http_call").apply {
                putAttribute("host", call.request().url.host)
                putAttribute("outcome", outcome)
                putMetric("total_ms", totalMs)
                putMetric("dns_ms", dnsMs)
                putMetric("connect_ms", connectMs)
                putMetric("tls_ms", tlsMs)
                putMetric("server_wait_ms", serverWaitMs)
                start()
                stop()
            }
        }
    }

    private fun now() = System.nanoTime()
    private fun msSince(start: Long) = (now() - start) / 1_000_000
}
