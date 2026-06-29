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

        abs(maxEpoch - landingEpoch) + 1
    } catch (e: Exception) {
        Logger.e("MissionInfoUtils", e) { "Error calculating Earth days active" }
        0L
    }
}

/**
 * Build timeline milestones for a rover mission.
 * @param rover The rover to build milestones for
 * @param landingLocation Landing site label shown alongside sol numbers, e.g. "Jezero Crater"
 * @return List of timeline milestones
 */
internal fun buildTimelineMilestones(rover: Rover, landingLocation: String = ""): List<TimelineMilestone> {
    // Strip the planet suffix for the compact sub-label ("Jezero Crater, Mars" → "Jezero Crater")
    val shortLocation = landingLocation.substringBefore(",").trim().takeIf { it.isNotEmpty() }
    val milestones = mutableListOf<TimelineMilestone>()

    // Launch
    milestones.add(
        TimelineMilestone(
            label = "Launch",
            date = formatDisplayDate(rover.launchDate),
            sol = null,
            type = MilestoneType.LAUNCH,
        )
    )

    // Landing
    milestones.add(
        TimelineMilestone(
            label = "Landing",
            date = formatDisplayDate(rover.landingDate),
            sol = 0,
            type = MilestoneType.LANDING,
            location = shortLocation,
        )
    )

    // Current / End
    val currentLabel = if (rover.status.equals("complete", ignoreCase = true)) "End" else "Current"
    val currentType  = if (rover.status.equals("complete", ignoreCase = true)) MilestoneType.END else MilestoneType.CURRENT
    val currentSubLabel = if (rover.isActive) shortLocation else null
    milestones.add(
        TimelineMilestone(
            label = currentLabel,
            date = formatDisplayDate(rover.maxDate),
            sol = rover.maxSol,
            type = currentType,
            location = currentSubLabel,
        )
    )

    return milestones
}
