package com.sirelon.marsroverphotos.feature.rovers

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.models.Rover

/**
 * @author romanishin
 * @since 07.11.16 on 12:42
 */
private const val SPIRIT = "spirit"
private const val CURIOSITY = "curiosity"
private const val OPPORTUNITY = "opportunity"
private const val INSIGHT = "insight"

class RoversRepository(context: Context) {

    private val pref: SharedPreferences =
        context.getSharedPreferences("MARS_ROVERS_PHOTOS_PREFS", Context.MODE_PRIVATE)
    private val gson: Gson = GsonBuilder().create()

    fun saveRover(rover: Rover) {
        pref.edit().putString(rover.name.toLowerCase(), gson.toJson(rover)).apply()
    }

    fun getRover(roverName: String): Rover {
        val roverNamePref = roverName.toLowerCase();

        val roverJson = pref.getString(roverNamePref, null)
        if (roverJson == null) {
            when (roverNamePref) {
                CURIOSITY -> return curiosity
                OPPORTUNITY -> return opportunity
                SPIRIT -> return spirit
                INSIGHT -> return insight
                else -> return curiosity
            }

        } else
            return gson.fromJson(roverJson, Rover::class.java)!!
    }

    fun getAllRovers(): MutableList<Rover> =
        mutableListOf(
            getRover(CURIOSITY), getRover(OPPORTUNITY), getRover(SPIRIT), getRover(INSIGHT)
        )

    private val resourcePrefix = "android.resource://com.sirelon.marsroverphotos/"

    private val curiosity by lazy {
        Rover(
            5,
            "Curiosity",
            resourcePrefix + R.drawable.img_curiosity,
            "2012-08-06",
            "2011-11-26",
            "active",
            1505,
            "2017-09-18",
            320999
        )
    }

    private val opportunity by lazy {
        Rover(
            6,
            "Opportunity",
            resourcePrefix + R.drawable.img_opportunity,
            "2004-01-25",
            "2003-07-07",
            "active",
            4535,
            "2017-02-22",
            187093
        )
    }

    private val spirit by lazy {
        Rover(
            7,
            "Spirit",
            resourcePrefix + R.drawable.img_spirit,
            "2004-01-04",
            "2003-06-10",
            "complete",
            2208,
            "2010-03-21",
            124550
        )
    }

    // TODO Fill in correct data
    private val insight by lazy {
        Rover(
            7,
            "Insight",
            resourcePrefix + R.drawable.img_spirit,
            "2004-01-04",
            "2003-06-10",
            "complete",
            2208,
            "2010-03-21",
            124550
        )
    }
}