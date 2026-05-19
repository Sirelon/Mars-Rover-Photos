package com.sirelon.marsroverphotos.platform

import com.google.firebase.crashlytics.FirebaseCrashlytics

actual fun recordException(t: Throwable) {
    try {
        FirebaseCrashlytics.getInstance().recordException(t)
    } catch (e: Exception) {
        // Firebase not configured
    }
}
