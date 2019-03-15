package com.sirelon.marsroverphotos.feature.rovers

import android.content.Context
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.extensions.logD
import com.sirelon.marsroverphotos.extensions.logE
import com.sirelon.marsroverphotos.models.Rover
import com.sirelon.marsroverphotos.models.RoverDateUtil
import com.sirelon.marsroverphotos.network.RoverInfo
import com.sirelon.marsroverphotos.storage.DataBaseProvider
import com.sirelon.marsroverphotos.storage.RoverDao
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers


const val INSIGHT_ID = 8L

/**
 * @author romanishin
 * @since 07.11.16 on 12:42
 */
class RoversRepository(context: Context) {

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
            93,
            "2019-02-02",
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
            "active",
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

        Single.fromCallable {
            roverDao.insertRovers(insight, curiosity, opportunity, spirit)
        }
            .subscribeOn(Schedulers.io())
            .doOnSuccess { "ROVERS INSERTED $it".logD() }
            .subscribe()
    }

    fun updateRoverCountPhotos(roverId: Long, photos: Long): Completable {
        "photos $photos".logD()
        return Completable.fromCallable { roverDao.updateRoverCountPhotos(roverId, photos) }
            .subscribeOn(Schedulers.io())
            .doOnError(Throwable::logE)
            .onErrorComplete()
    }

    fun getRoversObservable(): Observable<Rover> =
        roverDao.getRoversFlowable().toObservable().flatMapIterable { it }.doOnNext(Rover::logD)

    fun updateRoverByInfo(roverInfo: RoverInfo) {
        // We will not post status, 'cause it's incorrect data
        roverDao.updateRover(
            roverInfo.name,
            roverInfo.landingDate,
            roverInfo.launchDate,
            roverInfo.maxSol,
            roverInfo.maxDate,
            roverInfo.totalPhotos
        )
    }
}