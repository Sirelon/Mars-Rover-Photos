package com.sirelon.marsroverphotos.platform

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

/**
 * Android HTTP client engine using OkHttp
 */
actual fun createHttpClientEngine(): HttpClientEngine = OkHttp.create()
