package com.sirelon.marsroverphotos.platform

/**
 * Platform-agnostic interface for Firebase Analytics.
 */
expect class FirebaseAnalytics {
    /**
     * Log an event to analytics.
     * @param event Event name
     * @param parameters Event parameters
     */
    fun logEvent(event: String, parameters: Map<String, Any>)
}
