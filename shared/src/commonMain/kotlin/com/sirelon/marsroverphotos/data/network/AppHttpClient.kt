package com.sirelon.marsroverphotos.data.network

import com.sirelon.marsroverphotos.platform.createHttpClientEngine
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpMethod
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

/**
 * The app's HTTP client: the platform engine plus the request rules every platform shares.
 *
 * Create every [HttpClient] in the app through this function, so the per-host cap and the retry
 * apply on Android, iOS and desktop alike. The platform `createHttpClientEngine()` only picks the
 * engine; request behaviour configured in an engine applies to one platform only.
 */
fun createAppHttpClient(block: HttpClientConfig<*>.() -> Unit = {}): HttpClient =
    HttpClient(createHttpClientEngine()) {
        installHostRequestLimits()
        block()
    }

/**
 * Caps concurrent requests per host and retries a busy or failing server after a delay.
 *
 * A photo grid asks for dozens of images at once. planetarydata.jpl.nasa.gov (Viking) answers that
 * burst with 429 and 504 and multi-second latencies (SIR-107, SIR-172).
 *
 * The cap holds until the response headers arrive; the body download runs outside it. It is a
 * coroutine semaphore and not an engine setting: the image hosts speak HTTP/2, where Darwin's
 * `httpMaximumConnectionsPerHost` limits connections, not requests, and so caps almost nothing.
 *
 * Retry is installed first, so it wraps the cap: each attempt takes its own permit, and the wait
 * between attempts holds none. Only GET and HEAD are retried, on 429 and 5xx responses. Exceptions
 * (timeouts, no connection) are not retried; the callers already handle them.
 */
fun HttpClientConfig<*>.installHostRequestLimits(
    limiter: HostConcurrencyLimiter = HostConcurrencyLimiter.Shared,
    retryDelay: suspend (Long) -> Unit = { delay(it) },
) {
    install(HttpRequestRetry) {
        retryIf(maxRetries = MAX_RETRIES) { request, response ->
            request.method in RETRYABLE_METHODS && response.status.value.let { it == 429 || it in 500..599 }
        }
        retryOnExceptionIf { _, _ -> false }
        exponentialDelay(maxDelayMs = MAX_RETRY_DELAY_MS)
        delay(retryDelay)
    }
    install(HostConcurrencyLimit) {
        this.limiter = limiter
    }
}

/** Gives each host [maxRequestsPerHost] concurrent requests. */
class HostConcurrencyLimiter(private val maxRequestsPerHost: Int) {

    private val mutex = Mutex()
    private val semaphores = mutableMapOf<String, Semaphore>()

    suspend fun <T> withPermit(host: String, block: suspend () -> T): T {
        val semaphore = mutex.withLock { semaphores.getOrPut(host) { Semaphore(maxRequestsPerHost) } }
        return semaphore.withPermit { block() }
    }

    companion object {
        /** One process-wide limiter, so separate clients that hit the same host share the cap. */
        val Shared = HostConcurrencyLimiter(MAX_REQUESTS_PER_HOST)
    }
}

class HostConcurrencyLimitConfig {
    var limiter: HostConcurrencyLimiter = HostConcurrencyLimiter.Shared
}

private val HostConcurrencyLimit = createClientPlugin("HostConcurrencyLimit", ::HostConcurrencyLimitConfig) {
    val limiter = pluginConfig.limiter
    on(Send) { request ->
        limiter.withPermit(request.url.host) { proceed(request) }
    }
}

private const val MAX_REQUESTS_PER_HOST = 4
private const val MAX_RETRIES = 2
private const val MAX_RETRY_DELAY_MS = 10_000L
private val RETRYABLE_METHODS = setOf(HttpMethod.Get, HttpMethod.Head)
