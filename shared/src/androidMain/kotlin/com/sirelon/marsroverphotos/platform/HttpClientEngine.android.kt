package com.sirelon.marsroverphotos.platform

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

/**
 * Android HTTP client engine using OkHttp. Request rules live in `createAppHttpClient`.
 *
 * The event listener times each call's phases for SIR-167. It is Android-only because the stalls
 * it investigates are: see `RequestPhaseTimings`.
 */
actual fun createHttpClientEngine(): HttpClientEngine = OkHttp.create {
    config {
        eventListenerFactory { PhaseTimingEventListener(::reportToFirebasePerformance) }
    }
}
