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
}
