package com.sirelon.marsroverphotos.platform

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.Dispatcher

/**
 * Android HTTP client engine using OkHttp.
 *
 * Caps concurrent requests per host: a photo grid asks for dozens of images at once, and
 * planetarydata.jpl.nasa.gov (Viking) answers that burst with 504/429 and multi-second latencies.
 */
actual fun createHttpClientEngine(): HttpClientEngine = OkHttp.create {
    config {
        dispatcher(Dispatcher().apply { maxRequestsPerHost = MAX_REQUESTS_PER_HOST })
    }
}

private const val MAX_REQUESTS_PER_HOST = 4
