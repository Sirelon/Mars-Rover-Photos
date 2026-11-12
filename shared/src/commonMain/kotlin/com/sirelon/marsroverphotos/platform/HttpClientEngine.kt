package com.sirelon.marsroverphotos.platform

import io.ktor.client.engine.HttpClientEngine

/**
 * Platform-specific HTTP client engine for Ktor: OkHttp on Android and desktop, Darwin on iOS.
 * Every engine applies [MAX_REQUESTS_PER_HOST]; a setting added to one engine goes into all three.
 */
expect fun createHttpClientEngine(): HttpClientEngine

/** A photo grid asks for dozens of images at once; more than this per host gets 429s back. */
internal const val MAX_REQUESTS_PER_HOST = 4
