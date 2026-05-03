package com.sirelon.marsroverphotos.utils

import co.touchlab.kermit.Logger as KermitLogger

/**
 * Multiplatform logging utilities.
 * Uses Kermit for KMP logging.
 */

/**
 * Logger object providing a Timber-like API using Kermit.
 */
object Logger {
    private val kermit = KermitLogger.withTag("MarsRoverPhotos")

    fun d(tag: String, message: () -> String) {
        kermit.d(tag = tag, messageString = message())
    }

    fun d(tag: String, throwable: Throwable? = null, message: () -> String) {
        if (throwable != null) {
            kermit.d(tag = tag, throwable = throwable, messageString = message())
        } else {
            kermit.d(tag = tag, messageString = message())
        }
    }

    fun e(tag: String, throwable: Throwable?, message: () -> String) {
        kermit.e(tag = tag, throwable = throwable, messageString = message())
    }

    fun e(tag: String, message: String) {
        kermit.e(tag = tag, messageString = message)
    }

    fun i(tag: String, message: () -> String) {
        kermit.i(tag = tag, messageString = message())
    }

    fun w(tag: String, message: () -> String) {
        kermit.w(tag = tag, messageString = message())
    }

    fun v(tag: String, message: () -> String) {
        kermit.v(tag = tag, messageString = message())
    }
}

// Extension functions
fun Any?.logD(tag: String = "MarsRoverPhotos") {
    KermitLogger.d(tag) { this?.toString() ?: "NULL" }
}

fun Throwable.logE(tag: String = "MarsRoverPhotos") {
    KermitLogger.e(tag, this) { this.message ?: "Error occurred" }
}

fun recordException(e: Throwable) {
    e.printStackTrace()
    // Platform-specific crash reporting will be implemented via expect/actual
}
