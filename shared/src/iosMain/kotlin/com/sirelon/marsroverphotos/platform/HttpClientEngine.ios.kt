package com.sirelon.marsroverphotos.platform

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

/**
 * iOS HTTP client engine using Darwin (NSURLSession)
 */
actual fun createHttpClientEngine(): HttpClientEngine = Darwin.create()
