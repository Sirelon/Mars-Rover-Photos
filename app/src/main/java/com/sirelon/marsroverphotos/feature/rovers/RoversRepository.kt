package com.sirelon.marsroverphotos.feature.rovers

import android.content.Context
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.extensions.logD
import com.sirelon.marsroverphotos.models.Rover
import com.sirelon.marsroverphotos.models.RoverDateUtil
import com.sirelon.marsroverphotos.network.RestApi
import com.sirelon.marsroverphotos.network.RoverInfo
import com.sirelon.marsroverphotos.storage.DataBaseProvider
import com.sirelon.marsroverphotos.storage.RoverDao
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch


const val INSIGHT_ID = 4L

/**
 * @author romanishin
 * @since 07.11.16 on 12:42
 */
class RoversRepository(context: Context, private val api: RestApi) {

    private val roverDao: RoverDao

    init {
        DataBaseProvider.init(context)
        roverDao = DataBaseProvider.dataBase.roversDao()

        val resourcePrefix = "android.resource://com.sirelon.marsroverphotos/"
        val insight = Rover(
            INSIGHT_ID,
            "Insight",
            resourcePrefix + R.drawable.img_insight,
            "2018-11-26",
            "2018-05-05",
            "active",
            126,
            "2019-04-03",
            1072
        )

        val dateUtil = RoverDateUtil(insight)

        // Current date
        val currentTimeMillis = System.currentTimeMillis()
        insight.maxDate = dateUtil.parseTime(currentTimeMillis)
        insight.maxSol = dateUtil.solFromDate(currentTimeMillis)

        val curiosity = Rover(
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

        val opportunity = Rover(
            6,
            "Opportunity",
            resourcePrefix + R.drawable.img_opportunity,
            "2004-01-25",
            "2003-07-07",
            "complete",
            4535,
            "2017-02-22",
            187093
        )

        val spirit = Rover(
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

        GlobalScope.launch {
            val ids = roverDao.insertRovers(insight, curiosity, opportunity, spirit)

            "Inseerted $ids".logD()
        }
    }

    // Keep it for updating inforamttion via some time.
    private suspend fun loadFromServer() = coroutineScope{
        val roversName = listOf("curiosity", "opportunity", "spirit")
        val rovers = roversName.asFlow()
            .map { async {  api.getRoverInfo(it) } }
            .map { it.await() }
            .toList()
//        roverDao.insertRoversList(rovers)
    }

    suspend fun updateRoverCountPhotos(roverId: Long, photos: Long) {
        "photos $photos".logD()
        roverDao.updateRoverCountPhotos(roverId, photos)
    }

    fun getRovers() = roverDao.getRovers()

    fun updateRoversByInfo(list: List<RoverInfo>) {
//        DataBaseProvider.dataBase.runInTransaction {
        list.forEach(this::updateRoverByInfo)
//        }
    }

    fun updateRoverByInfo(roverInfo: RoverInfo) {
        "UPDATE ROVER BY INFO $roverInfo".logD()
        // We will not post status, 'cause it's incorrect data
        kotlin.runCatching {
            roverDao.updateRover(
                roverInfo.name,
                roverInfo.landingDate,
                roverInfo.launchDate,
                roverInfo.maxSol,
                roverInfo.maxDate,
                roverInfo.totalPhotos
            )
        }.onFailure(Throwable::printStackTrace)
    }
}

