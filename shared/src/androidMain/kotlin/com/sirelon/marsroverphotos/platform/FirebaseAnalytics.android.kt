package com.sirelon.marsroverphotos.platform

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics as GoogleFirebaseAnalytics

/**
 * Android implementation of FirebaseAnalytics.
 */
actual class FirebaseAnalytics(
    private val analytics: GoogleFirebaseAnalytics
) {
    actual fun logEvent(event: String, parameters: Map<String, Any>) {
        val bundle = Bundle().apply {
            parameters.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    is Boolean -> putBoolean(key, value)
                    else -> putString(key, value.toString())
                }
            }
        }
        analytics.logEvent(event, bundle)
    }
}
