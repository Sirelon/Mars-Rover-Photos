package com.sirelon.marsroverphotos.platform

/**
 * Platform abstraction for triggering an in-app review prompt.
 *
 * Returns `true` if the platform's review overlay was shown.
 * Returns `false` on unsupported platforms or when the overlay cannot be shown —
 * callers should fall back to opening the store URL.
 */
interface AppReview {
    suspend fun requestReview(): Boolean
}

/** No-op implementation used on iOS and Desktop. */
class NoOpAppReview : AppReview {
    override suspend fun requestReview(): Boolean = false
}
