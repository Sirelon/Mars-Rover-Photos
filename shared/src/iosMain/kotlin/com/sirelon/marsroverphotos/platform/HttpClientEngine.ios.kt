package com.sirelon.marsroverphotos.platform

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

actual fun createHttpClientEngine(): HttpClientEngine = Darwin.create {
    configureSession {
        HTTPMaximumConnectionsPerHost = MAX_REQUESTS_PER_HOST.toLong()
    }
}
