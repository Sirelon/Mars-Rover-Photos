package com.sirelon.marsroverphotos.platform

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.crashlytics.crashlytics

actual fun recordException(t: Throwable) {
    try {
        Firebase.crashlytics.recordException(t)
    } catch (e: Exception) {
        // Firebase not configured
    }
}
