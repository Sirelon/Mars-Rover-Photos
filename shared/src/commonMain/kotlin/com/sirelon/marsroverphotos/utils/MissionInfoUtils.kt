package com.sirelon.marsroverphotos.utils

import com.sirelon.marsroverphotos.domain.models.Rover
import com.sirelon.marsroverphotos.presentation.viewmodels.MilestoneType
import com.sirelon.marsroverphotos.presentation.viewmodels.TimelineMilestone
import kotlinx.datetime.LocalDate
import kotlin.math.abs

/**
 * Calculate the number of Earth days a rover has been active.
 * @param landingDate Landing date in yyyy-MM-dd format
 * @param maxDate Maximum date in yyyy-MM-dd format
 * @return Number of Earth days active
 */
internal fun calculateEarthDaysActive(landingDate: String, maxDate: String): Long {
    return try {
        val landing = LocalDate.parse(landingDate)
        val max = LocalDate.parse(maxDate)

        // Calculate difference in days
        val landingEpoch = landing.toEpochDays()
        val maxEpoch = max.toEpochDays()

        (abs(maxEpoch - landingEpoch) + 1).toLong()
    } catch (e: Exception) {
        Logger.e("MissionInfoUtils", e) { "Error calculating Earth days active" }
        0L
    }
}

/**
 * Build timeline milestones for a rover mission.
 * @param rover The rover to build milestones for
 * @return List of timeline milestones
 */
internal fun buildTimelineMilestones(rover: Rover): List<TimelineMilestone> {
    val milestones = mutableListOf<TimelineMilestone>()

    // Launch
    milestones.add(
        TimelineMilestone(
            label = "Launch",
            date = rover.launchDate,
            sol = null,
            type = MilestoneType.LAUNCH
        )
    )

    // Landing
    milestones.add(
        TimelineMilestone(
            label = "Landing",
            date = rover.landingDate,
            sol = 0,
            type = MilestoneType.LANDING
        )
    )

    // Current
    milestones.add(
        TimelineMilestone(
            label = "Current",
            date = rover.maxDate,
            sol = rover.maxSol,
            type = MilestoneType.CURRENT
        )
    )

    // End (only if mission is complete)
    if (rover.status.equals("complete", ignoreCase = true)) {
        milestones.add(
            TimelineMilestone(
                label = "End",
                date = rover.maxDate,
                sol = rover.maxSol,
                type = MilestoneType.END
            )
        )
    }

    return milestones
}
