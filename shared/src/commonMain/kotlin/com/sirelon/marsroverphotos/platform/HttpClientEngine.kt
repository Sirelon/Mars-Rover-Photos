package com.sirelon.marsroverphotos.platform

import io.ktor.client.engine.HttpClientEngine

/**
 * Platform-specific HTTP client engine for Ktor: OkHttp on Android and desktop, Darwin on iOS.
 *
 * Only the engine choice belongs here. Request behaviour — per-host limits, retries, timeouts —
 * goes in `createAppHttpClient` (commonMain), so it applies on every platform. Call that, not
 * `HttpClient(createHttpClientEngine())`.
 */
expect fun createHttpClientEngine(): HttpClientEngine
