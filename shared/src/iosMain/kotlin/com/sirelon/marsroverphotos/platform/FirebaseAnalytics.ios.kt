package com.sirelon.marsroverphotos.platform

/**
 * iOS implementation of FirebaseAnalytics.
 * TODO: Implement with Firebase iOS SDK
 */
actual class FirebaseAnalytics {
    actual fun logEvent(event: String, parameters: Map<String, Any>) {
        // TODO: Implement Firebase iOS SDK
        println("Analytics event: $event with parameters: $parameters")
    }
}
