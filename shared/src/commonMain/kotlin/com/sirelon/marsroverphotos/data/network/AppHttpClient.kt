package com.sirelon.marsroverphotos.data.network

import com.sirelon.marsroverphotos.platform.createHttpClientEngine
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpRequestRetry

/**
 * The app's HTTP client. A busy server (planetarydata.jpl.nasa.gov answers a photo-grid burst with
 * 429 and 5xx) gets two more tries after a growing delay. Failed connections and timeouts are not
 * retried: a timeout already took two minutes.
 */
fun createAppHttpClient(block: HttpClientConfig<*>.() -> Unit = {}): HttpClient =
    HttpClient(createHttpClientEngine()) {
        install(HttpRequestRetry) {
            retryIf(maxRetries = 2) { _, response -> response.status.value == 429 || response.status.value >= 500 }
            retryOnExceptionIf { _, _ -> false }
            exponentialDelay(maxDelayMs = 10_000)
        }
        block()
    }
