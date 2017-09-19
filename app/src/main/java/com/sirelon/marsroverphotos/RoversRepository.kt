package com.sirelon.marsroverphotos

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.sirelon.marsroverphotos.models.Rover

/**
 * @author romanishin
 * @since 07.11.16 on 12:42
 */
class RoversRepository(context: Context) {

    val pref: SharedPreferences = context.getSharedPreferences("MARS_ROVERS_PHOTOS_PREFS", Context.MODE_PRIVATE)
    val gson: Gson = GsonBuilder().create()

    fun saveRover(rover: Rover) {
        pref.edit().putString(rover.name.toLowerCase(), gson.toJson(rover)).apply();
    }

    fun getRover(roverName: String): Rover {
        val roverNamePref = roverName.toLowerCase();

        val roverJson = pref.getString(roverNamePref, null)
        if (roverJson == null) {
            when (roverNamePref) {
                "curiosity" -> return curiosity
                "opportunity" -> return opportunity
                "spirit" -> return spirit
                else -> return curiosity
            }

        } else
            return gson.fromJson(roverJson, Rover::class.java)!!
    }

    fun getAllRovers(): MutableList<Rover> =
            mutableListOf(getRover("curiosity"), getRover("opportunity"), getRover("spirit"))

    private val resourcePrefix = "android.resource://com.sirelon.marsroverphotos/"

    val curiosity by lazy {
        Rover(
                5,
                "Curiosity",
                resourcePrefix + R.drawable.img_curiosity,
                "2012-08-06",
                "2011-11-26",
                "active",
                1505,
                "2016-10-30",
                285665
        )
    }

    val opportunity by lazy {
        Rover(
                6,
                "Opportunity",
                resourcePrefix + R.drawable.img_opportunity,
                "2004-01-25",
                "2003-07-07",
                "active",
                4535,
                "2016-10-27",
                184544
        )
    }

    val spirit by lazy {
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
}