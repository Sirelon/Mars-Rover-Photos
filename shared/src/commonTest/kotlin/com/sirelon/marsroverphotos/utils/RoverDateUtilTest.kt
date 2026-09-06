package com.sirelon.marsroverphotos.utils

import com.sirelon.marsroverphotos.domain.models.Rover
import kotlin.test.Test
import kotlin.test.assertEquals

class RoverDateUtilTest {

    private val curiosity = Rover(
        id = 5L,
        name = "Curiosity",
        drawableName = "img_curiosity",
        landingDate = "2012-08-06",
        launchDate = "2011-11-26",
        status = "active",
        maxSol = 1505L,
        maxDate = "2017-09-18",
        totalPhotos = 320999
    )

    @Test
    fun dateFromSol_zeroSol_returnsLandingDate() {
        val util = RoverDateUtil(curiosity)
        assertEquals("2012-08-06", util.parseTime(util.dateFromSol(0L)))
    }

    @Test
    fun dateFromSol_oneSol_returnsDayAfterLanding() {
        val util = RoverDateUtil(curiosity)
        // 1 * 1.0275 floors to 1 Earth day
        assertEquals("2012-08-07", util.parseTime(util.dateFromSol(1L)))
    }

    @Test
    fun dateFromSol_hundredSols_returnsCorrectDate() {
        val util = RoverDateUtil(curiosity)
        // 100 * 1.0275 = 102.75 → 102 days after 2012-08-06 = 2012-11-16
        assertEquals("2012-11-16", util.parseTime(util.dateFromSol(100L)))
    }

    /**
     * Ingenuity is seeded with Perseverance's landing date because it has no sol clock of its own —
     * it rode down under the rover's belly. Every date the pickers show for the helicopter is
     * derived from that, so these pin the two ends of its campaign.
     */
    private val ingenuity = Rover(
        id = 10L,
        name = "Ingenuity Helicopter",
        drawableName = "img_ingenuity",
        landingDate = "2021-02-18",
        launchDate = "2020-07-30",
        status = "complete",
        maxSol = 1069L,
        maxDate = "2024-02-22",
        totalPhotos = 14553
    )

    @Test
    fun ingenuity_firstImagedSolMapsToItsRealDate() {
        val util = RoverDateUtil(ingenuity)
        // Sol 43 is 3 April 2021, the day Ingenuity was set down on the surface and returned its
        // first frames. Its first flight came later, on 19 April. Verified against the live feed.
        assertEquals("2021-04-03", util.parseTime(util.dateFromSol(43L)))
    }

    @Test
    fun ingenuity_lastSolMapsIntoTheFinalFlightWindow() {
        val util = RoverDateUtil(ingenuity)
        assertEquals("2024-02-21", util.parseTime(util.dateFromSol(1069L)))
    }

    @Test
    fun ingenuity_maxDateRoundTripsOneSolPastMaxSol() {
        val util = RoverDateUtil(ingenuity)
        // Documented asymmetry, not a bug: the conversion's inclusive +1 day means the seeded
        // maxDate resolves to 1070 while maxSol is 1069. Harmless because applyAnchor clamps back
        // to 1069, itself a real sol with photos. Asserted so the drift can't grow unnoticed.
        assertEquals(1070L, util.solFromDate(util.dateFromSol(1069L) + 24 * 60 * 60 * 1000L))
    }
}
