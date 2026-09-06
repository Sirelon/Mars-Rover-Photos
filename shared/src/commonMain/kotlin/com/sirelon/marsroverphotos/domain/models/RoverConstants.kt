package com.sirelon.marsroverphotos.domain.models

/**
 * Rover ID constants
 */
const val CURIOSITY_ID = 5L
const val OPPORTUNITY_ID = 6L
const val SPIRIT_ID = 7L
const val INSIGHT_ID = 4L
const val PERSEVERANCE_ID = 3L
const val VIKING_1_ID = 8L
const val VIKING_2_ID = 9L
const val INGENUITY_ID = 10L

/**
 * Order the missions are listed in.
 *
 * `RoverDao.getRovers()` is an unordered `SELECT`, and `id` is an `INTEGER PRIMARY KEY` — a rowid
 * alias — so rows come back in ascending id order. That put every new mission at the bottom.
 * Ingenuity belongs beside Perseverance instead: it rode down under its belly and shares its sol
 * clock. Sorting here keeps that placement without a schema change, and every screen reading the
 * roster inherits it. Ids absent from this list sort last.
 */
val ROVER_DISPLAY_ORDER: List<Long> = listOf(
    PERSEVERANCE_ID,
    INGENUITY_ID,
    INSIGHT_ID,
    CURIOSITY_ID,
    OPPORTUNITY_ID,
    SPIRIT_ID,
    VIKING_1_ID,
    VIKING_2_ID,
)

/** Sorts a roster into [ROVER_DISPLAY_ORDER]. Missions absent from that list sort last. */
fun List<Rover>.inDisplayOrder(): List<Rover> = sortedBy { rover ->
    ROVER_DISPLAY_ORDER.indexOf(rover.id).takeIf { it >= 0 } ?: Int.MAX_VALUE
}
