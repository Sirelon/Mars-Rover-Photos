package com.sirelon.marsroverphotos.domain.repositories

import kotlinx.coroutines.flow.StateFlow

/**
 * The photo each rover card on the rovers list shows: the rover's latest real photo, by rover id.
 */
interface RoverCoversRepository {
    /**
     * Image URL per rover id. Starts with the URLs saved by an earlier launch, so a cold start
     * shows the last known photos at once. A rover with no known photo is absent from the map.
     */
    val coverUrls: StateFlow<Map<Long, String>>

    /**
     * Fetches every rover's latest photo and updates [coverUrls] as each one arrives. Only the
     * first call in a process does anything. A failed fetch keeps the rover's saved URL.
     */
    fun refreshOnce()
}
