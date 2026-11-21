package com.sirelon.marsroverphotos.domain.repositories

import kotlinx.coroutines.flow.StateFlow

/**
 * The photo each rover card on the rovers list shows: the rover's latest real photo, by rover id.
 * The freshest of them also feeds the "Today on Mars" card at the top of that list.
 */
interface RoverCoversRepository {
    /**
     * Latest photo per rover id. Starts with the photos saved by an earlier launch, so a cold
     * start shows the last known photos at once. A rover with no known photo is absent from the
     * map.
     */
    val covers: StateFlow<Map<Long, Cover>>

    /**
     * Fetches every rover's latest photo and updates [covers] as each one arrives. Only the
     * first call in a process does anything. A failed fetch keeps the rover's saved photo.
     */
    fun refreshOnce()

    /**
     * A rover's latest photo. [sol] is 0 and [earthDate] blank when unknown: covers saved by
     * builds that stored only the URL.
     */
    data class Cover(
        val imageUrl: String,
        val sol: Long = 0,
        val earthDate: String = "",
    )
}
