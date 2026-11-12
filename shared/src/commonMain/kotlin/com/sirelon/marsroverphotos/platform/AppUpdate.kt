package com.sirelon.marsroverphotos.platform

/**
 * Platform-agnostic in-app update launcher.
 *
 * Android wires this to Google Play's in-app update flow (Play Core). Other platforms have no
 * such flow and return `false`, so the caller opens the store listing.
 */
interface AppUpdate {
    /**
     * Starts the platform's own update flow for a newer store version.
     *
     * @return `true` when the platform took the request: the update ran, or the user declined
     *         it on the platform's screen. `false` when no in-app flow is available (no update
     *         offered, no store backing, an error) — the caller should open the store listing.
     */
    suspend fun startUpdate(): Boolean
}

/** Used by targets without an in-app update flow. */
class NoOpAppUpdate : AppUpdate {
    override suspend fun startUpdate(): Boolean = false
}
