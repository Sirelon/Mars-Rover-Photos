package com.sirelon.marsroverphotos.models

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * @author romanishin
 * @since 02.11.16 on 16:50
 */
class RoverDateUtil(val rover: Rover) {

    private val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val SOL_TO_DAY_OFFSET = 1.0275

    val roverLandingDate: Long
        get() {
            return if (_roverLandingDate < _roverLastDate){
                _roverLandingDate
            } else {
                _roverLastDate
            }
        }

    val roverLastDate: Long
        get() {
            return if (_roverLastDate > _roverLandingDate){
                _roverLastDate
            } else {
                _roverLandingDate
            }
        }

    val _roverLastDate: Long by lazy {
        try {
            simpleDateFormat.parse(rover.maxDate).time
        } catch (e: Exception) {
            0.toLong()
        }
    }

    val _roverLandingDate: Long by lazy {
        try {
            simpleDateFormat.parse(rover.landingDate).time
        } catch (e: Exception) {
            0.toLong()
        }
    }

    fun parseTime(time: Long): String = simpleDateFormat.format(time)

    fun solFromDate(date: Long): Long {
        // + include end and curent days
        val days = (date - roverLandingDate) / (1000 * 60 * 60 * 24) + 1

        val sol = days / SOL_TO_DAY_OFFSET

        Log.d("Sirelon", "DEFERENCE BETWEEN DATES EARTH DAYS = $days SOL = $sol")

        return sol.toLong()
    }

    fun dateFromSol(sol: Long): Long {
        val days = sol * SOL_TO_DAY_OFFSET
        val daysTime = TimeUnit.DAYS.toMillis(days.toLong())
        return roverLandingDate + daysTime
    }
}