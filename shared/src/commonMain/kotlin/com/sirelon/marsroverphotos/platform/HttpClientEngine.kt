package com.sirelon.marsroverphotos.platform

import io.ktor.client.engine.HttpClientEngine

/**
 * Platform-specific HTTP client engine for Ktor.
 * Each platform will provide its own engine implementation:
 * - Android/Desktop: OkHttp
 * - iOS: Darwin
 * - Web: JS
 */
expect fun createHttpClientEngine(): HttpClientEngine
