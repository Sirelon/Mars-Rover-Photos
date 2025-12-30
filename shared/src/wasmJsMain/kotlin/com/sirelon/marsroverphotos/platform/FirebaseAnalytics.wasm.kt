package com.sirelon.marsroverphotos.platform

/**
 * Web implementation of FirebaseAnalytics.
 * TODO: Implement with Firebase Web SDK
 */
actual class FirebaseAnalytics {
    actual fun logEvent(event: String, parameters: Map<String, Any>) {
        // TODO: Implement Firebase Web SDK
        console.log("Analytics event: $event with parameters: $parameters")
    }
}
