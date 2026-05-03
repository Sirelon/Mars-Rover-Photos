package com.sirelon.marsroverphotos.platform

/**
 * Desktop implementation of FirebaseAnalytics.
 * Stub implementation - Firebase not available on desktop.
 */
actual class FirebaseAnalytics {
    actual fun logEvent(event: String, parameters: Map<String, Any>) {
        // No-op for desktop
        println("Analytics event (desktop stub): $event")
    }
}
