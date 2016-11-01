package com.sirelon.marsroverphotos

import com.sirelon.marsroverphotos.models.MarsPhoto
import com.sirelon.marsroverphotos.models.PhotosQueryRequest
import com.sirelon.marsroverphotos.models.Rover
import com.sirelon.marsroverphotos.network.RestApi
import io.reactivex.Observable

/**
 * @author romanishin
 * @since 31.10.16 on 12:43
 */
class DataManager(private val api: RestApi = RestApi()) {

    fun getMarsPhotos(queryRequest: PhotosQueryRequest): Observable<List<MarsPhoto>> {
        return Observable.create {
            subscriber ->

            val callResponse = api.getRoversPhotos(queryRequest)
            val response = callResponse.execute()
            if (response.isSuccessful) {
                val marsPhotos = response.body().photos
                subscriber.onNext(marsPhotos)
                subscriber.onComplete()
            } else
                subscriber.onError(Throwable(response.message()))
        }
    }

    fun getRovers(): Observable<Rover> {
        return Observable.fromIterable(localRovers());


//        return Observable.create {
//            subscriber ->
//            subscriber.onNext(localRovers())
//
////            val callResponse = api.getRoverPhotos(1000, null)
////            val response = callResponse.execute()
////            if (response.isSuccessful) {
////                val marsPhotos = response.body().photos
////                subscriber.onNext(marsPhotos)
////                subscriber.onComplete()
////            } else
////                subscriber.onError(Throwable(response.message()))
//        }
    }

    private fun localRovers(): List<Rover> {
        return mutableListOf(
                Rover(
                        5,
                        "Curiosity",
                        "http://estaticos.muyinteresante.es/uploads/images/article/55365b6b34099b0279c8fa99/marte-curiosity.jpg",
                        "2012-08-06",
                        "2011-11-26",
                        "active",
                        1505,
                        "2016-10-30",
                        285544
                ),
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
                ),
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
        )
    }
}