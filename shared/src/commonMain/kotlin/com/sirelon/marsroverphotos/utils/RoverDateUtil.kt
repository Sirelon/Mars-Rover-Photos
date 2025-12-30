package com.sirelon.marsroverphotos.utils

import com.sirelon.marsroverphotos.domain.models.Rover
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days

/**
 * Utility class for converting between Earth dates and Mars sols.
 * A Mars sol is approximately 1.0275 Earth days.
 *
 * @author romanishin
 * @since 02.11.16 on 16:50
 */
private const val SolToDayOffset = 1.0275

class RoverDateUtil(private val rover: Rover) {

    private val dateFormat = "yyyy-MM-dd"

    val roverLandingDate: Long
        get() = minOf(_roverLandingDate, _roverLastDate)

    val roverLastDate: Long
        get() = maxOf(_roverLastDate, _roverLandingDate)

    private val _roverLastDate: Long by lazy {
        parseDate(rover.maxDate)
    }

    private val _roverLandingDate: Long by lazy {
        parseDate(rover.landingDate)
    }

    /**
     * Parse date string in format "yyyy-MM-dd" to milliseconds.
     */
    private fun parseDate(dateString: String): Long {
        return try {
            val localDate = LocalDate.parse(dateString)
            localDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        } catch (e: Exception) {
            Logger.e("RoverDateUtil", e) { "Failed to parse date: $dateString" }
            0L
        }
    }

    /**
     * Format time in milliseconds to date string "yyyy-MM-dd".
     */
    fun parseTime(time: Long): String {
        return try {
            val instant = Instant.fromEpochMilliseconds(time)
            val localDate = instant.toLocalDateTime(TimeZone.UTC).date
            localDate.toString() // Returns yyyy-MM-dd format
        } catch (e: Exception) {
            Logger.e("RoverDateUtil", e) { "Failed to format time: $time" }
            ""
        }
    }

    /**
     * Calculate Mars sol from Earth date.
     * @param date Earth date in milliseconds
     * @return Mars sol number
     */
    fun solFromDate(date: Long): Long {
        // Include current day in the calculation
        val daysDiff = (date - roverLandingDate) / (1000 * 60 * 60 * 24) + 1
        val sol = daysDiff / SolToDayOffset
        return sol.toLong()
    }

    /**
     * Calculate Earth date from Mars sol.
     * @param sol Mars sol number
     * @return Earth date in milliseconds
     */
    fun dateFromSol(sol: Long): Long {
        val days = sol * SolToDayOffset
        val daysMillis = days.toLong().days.inWholeMilliseconds
        return roverLandingDate + daysMillis
    }
}
