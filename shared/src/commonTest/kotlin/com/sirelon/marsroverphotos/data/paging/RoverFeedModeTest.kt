package com.sirelon.marsroverphotos.data.paging

import com.sirelon.marsroverphotos.domain.models.CURIOSITY_ID
import com.sirelon.marsroverphotos.domain.models.INSIGHT_ID
import com.sirelon.marsroverphotos.domain.models.OPPORTUNITY_ID
import com.sirelon.marsroverphotos.domain.models.PERSEVERANCE_ID
import com.sirelon.marsroverphotos.domain.models.SPIRIT_ID
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RoverFeedModeTest {

    @Test
    fun spiritAndOpportunity_usePageFeed() {
        assertTrue(SPIRIT_ID.usesPageFeed())
        assertTrue(OPPORTUNITY_ID.usesPageFeed())
    }

    @Test
    fun curiosityInsightPerseverance_useSolFeed() {
        assertFalse(CURIOSITY_ID.usesPageFeed())
        assertFalse(INSIGHT_ID.usesPageFeed())
        assertFalse(PERSEVERANCE_ID.usesPageFeed())
    }

    @Test
    fun pageQuery_returnsCorrectQueriesForMerRovers() {
        assertTrue(SPIRIT_ID.pageQuery().contains("Spirit", ignoreCase = true))
        assertTrue(OPPORTUNITY_ID.pageQuery().contains("Opportunity", ignoreCase = true))
    }
}
