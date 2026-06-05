package com.sirelon.marsroverphotos.domain.models

/**
 * Rover ID constants
 */
const val CURIOSITY_ID = 5L
const val OPPORTUNITY_ID = 6L
const val SPIRIT_ID = 7L
const val INSIGHT_ID = 4L
const val PERSEVERANCE_ID = 3L

/**
 * Rovers whose APIs use page-number pagination rather than sol-number pagination.
 * Perseverance and Insight expose a raw-images feed ordered by sol desc with a `page` parameter;
 * they do NOT reliably support per-sol filtering with a single page=0 call.
 */
fun isPageBased(roverId: Long): Boolean =
    roverId == PERSEVERANCE_ID || roverId == INSIGHT_ID

/**
 * Items returned per API page for each page-based rover.
 */
fun perPageFor(roverId: Long): Int = when (roverId) {
    PERSEVERANCE_ID -> 100
    INSIGHT_ID -> 100
    else -> 0
}
