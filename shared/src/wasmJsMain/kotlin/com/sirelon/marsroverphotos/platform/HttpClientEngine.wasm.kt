package com.sirelon.marsroverphotos.platform

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.js.Js

/**
 * Web HTTP client engine using JS (Fetch API)
 */
actual fun createHttpClientEngine(): HttpClientEngine = Js.create()
