package com.sirelon.marsroverphotos.platform

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.analytics.analytics
import dev.gitlive.firebase.analytics.logEvent

/**
 * iOS implementation of FirebaseAnalytics using GitLive KMP SDK.
 */
actual class FirebaseAnalytics {
    actual fun logEvent(event: String, parameters: Map<String, Any>) {
        Firebase.analytics.logEvent(event) {
            parameters.forEach { (key, value) ->
                when (value) {
                    is String -> param(key, value)
                    is Long -> param(key, value)
                    is Int -> param(key, value.toLong())
                    is Double -> param(key, value)
                    is Boolean -> param(key, if (value) 1L else 0L)
                    else -> param(key, value.toString())
                }
            }
        }
    }
}
