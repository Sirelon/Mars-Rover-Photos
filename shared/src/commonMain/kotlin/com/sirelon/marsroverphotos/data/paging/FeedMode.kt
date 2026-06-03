package com.sirelon.marsroverphotos.data.paging

import com.sirelon.marsroverphotos.domain.models.OPPORTUNITY_ID
import com.sirelon.marsroverphotos.domain.models.SPIRIT_ID

/**
 * Discriminates how the rover photo feed is sourced.
 */
sealed interface FeedMode {
    data class Sol(
        val anchorSol: Long,
        val minSol: Long,
        val maxSol: Long,
        val cameras: Set<String>,
    ) : FeedMode

    data class Page(val query: String) : FeedMode
}

/** Returns true when this rover id should use a page-keyed feed instead of the sol-keyed feed. */
fun Long.usesPageFeed(): Boolean = this == SPIRIT_ID || this == OPPORTUNITY_ID

/**
 * Returns the images.nasa.gov search query for this page-feed rover.
 * Only valid for rover IDs where [usesPageFeed] is true.
 */
fun Long.pageQuery(): String = when (this) {
    SPIRIT_ID -> "Spirit rover Mars"
    OPPORTUNITY_ID -> "Opportunity rover Mars"
    else -> error("pageQuery() called for non-page-feed rover id=$this")
}
