package com.sirelon.marsroverphotos.feature.mission

import com.sirelon.marsroverphotos.models.Rover
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import timber.log.Timber

internal fun calculateEarthDaysActive(landingDate: String, maxDate: String): Long {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return try {
        val landingMillis = dateFormat.parse(landingDate)?.time ?: 0L
        val maxMillis = dateFormat.parse(maxDate)?.time ?: 0L

        if (landingMillis > 0 && maxMillis > 0) {
            TimeUnit.MILLISECONDS.toDays(maxMillis - landingMillis) + 1
        } else {
            0L
        }
    } catch (e: Exception) {
        Timber.e(e, "Error calculating Earth days active")
        0L
    }
}

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
