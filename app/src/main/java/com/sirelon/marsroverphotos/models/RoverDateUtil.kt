package com.sirelon.marsroverphotos.models

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * @author romanishin
 * @since 02.11.16 on 16:50
 */
private const val SolToDayOffset = 1.0275

class RoverDateUtil(val rover: Rover) {

    private val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

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

    private val _roverLastDate: Long by lazy {
        try {
            simpleDateFormat.parse(rover.maxDate).time
        } catch (e: Exception) {
            e.printStackTrace()
            0.toLong()
        }
    }

    private val _roverLandingDate: Long by lazy {
        try {
            simpleDateFormat.parse(rover.landingDate).time
        } catch (e: Exception) {
            e.printStackTrace()
            0.toLong()
        }
    }

    fun parseTime(time: Long): String = simpleDateFormat.format(time)

    fun solFromDate(date: Long): Long {
        // + include end and curent days
        val days = (date - roverLandingDate) / (1000 * 60 * 60 * 24) + 1

        val sol = days / SolToDayOffset

        return sol.toLong()
    }

    fun dateFromSol(sol: Long): Long {
        val days = sol * SolToDayOffset
        val daysTime = TimeUnit.DAYS.toMillis(days.toLong())
        return roverLandingDate + daysTime
    }
}