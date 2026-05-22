package com.sirelon.marsroverphotos.platform

/**
 * Platform-agnostic in-app review launcher.
 *
 * Android wires this to Google Play's in-app review flow (Play Core).
 * Other platforms return [false] so the caller can fall back to opening the store URL.
 */
interface AppReview {
    /**
     * Attempts to show an in-app review prompt.
     *
     * @return `true` if the platform's in-app review flow was launched (or attempted)
     *         and the caller should NOT redirect to the store; `false` if the platform
     *         doesn't support in-app review or the flow could not be shown — the caller
     *         should fall back to the store URL.
     */
    suspend fun requestReview(): Boolean
}

/**
 * Common no-op implementation used by non-Android targets.
 */
class NoOpAppReview : AppReview {
    override suspend fun requestReview(): Boolean = false
}
