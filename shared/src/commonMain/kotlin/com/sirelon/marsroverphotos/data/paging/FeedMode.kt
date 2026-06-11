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

    /**
     * Page-keyed feed (Spirit/Opportunity). [anchorPage] is the 1-based API page to open at;
     * null lets the paging source pick a random page — the page-feed counterpart of the sol
     * feed's random-sol anchor.
     */
    data class Page(val query: String, val anchorPage: Int? = null) : FeedMode
}

/** Returns true when this rover id should use a page-keyed feed instead of the sol-keyed feed. */
fun Long.usesPageFeed(): Boolean = this == SPIRIT_ID || this == OPPORTUNITY_ID

/**
 * Returns the images.nasa.gov search query for this page-feed rover. Combined with
 * [MER_KEYWORDS] for precision, the bare rover name yields the largest relevant gallery
 * (Spirit: 434 vs 267 hits for "Spirit rover", verified 2026-06-12).
 * Only valid for rover IDs where [usesPageFeed] is true.
 */
fun Long.pageQuery(): String = when (this) {
    SPIRIT_ID -> "Spirit"
    OPPORTUNITY_ID -> "Opportunity"
    else -> error("pageQuery() called for non-page-feed rover id=$this")
}

/**
 * images.nasa.gov `keywords` filter sent with every page-feed search. Keeps the broad
 * [pageQuery] precise — "Spirit"/"Opportunity" alone match unrelated media.
 */
const val MER_KEYWORDS = "Mars Exploration Rover"
