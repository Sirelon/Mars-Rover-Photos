package com.sirelon.marsroverphotos

import android.content.Context
import android.content.SharedPreferences
import com.sirelon.marsroverphotos.models.Rover
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi

/**
 * @author romanishin
 * @since 07.11.16 on 12:42
 */
class RoversRepository(context: Context) {

    val pref: SharedPreferences
    val moshiAdapter: JsonAdapter<Rover>

    init {
        pref = context.getSharedPreferences("MARS_ROVERS_PHOTOS_PREFS", Context.MODE_PRIVATE)
        val moshi = Moshi.Builder().build()
        moshiAdapter = moshi.adapter(Rover::class.java)
    }

    fun saveRover(rover: Rover) {
        pref.edit().putString(rover.name.toLowerCase(), moshiAdapter.toJson(rover)).apply();
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
            return moshiAdapter.fromJson(roverJson)!!
    }

    fun getAllRovers(): MutableList<Rover> =
            mutableListOf(getRover("curiosity"), getRover("opportunity"), getRover("spirit"))

    val curiosity by lazy {
        Rover(
                5,
                "Curiosity",
                "http://estaticos.muyinteresante.es/uploads/images/article/55365b6b34099b0279c8fa99/marte-curiosity.jpg",
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
                "http://www.spaceflightinsider.com/wp-content/uploads/2015/01/Mars-Exploration-Rover-Spirit-Opportunity-surface-of-Red-Planet-NASA-image-posted-on-SpaceFlight-Insider-647x518.jpg",
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
                "http://www.exploratorium.edu/mars/images/rover1_br.jpg",
                "2004-01-04",
                "2003-06-10",
                "complete",
                2208,
                "2010-03-21",
                124550
        )
    }
}