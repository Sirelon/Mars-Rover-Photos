package com.sirelon.marsroverphotos

import android.content.Context
import android.util.Log
import com.sirelon.marsroverphotos.models.MarsPhoto
import com.sirelon.marsroverphotos.models.PhotosQueryRequest
import com.sirelon.marsroverphotos.models.Rover
import com.sirelon.marsroverphotos.network.RestApi
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

/**
 * @author romanishin
 * @since 31.10.16 on 12:43
 */
class DataManager(val context: Context, private val api: RestApi = RestApi()) {

    var lastPhotosRequest: Observable<MutableList<MarsPhoto>?>? = null

    fun loadMarsPhotos(queryRequest: PhotosQueryRequest): Observable<MutableList<MarsPhoto>> {
//        val mainObserver =
        return Observable.fromCallable {
            val callResponse = api.getRoversPhotos(queryRequest)
            val response = callResponse.execute()
            Log.e("Sirelon", "loadMarsPhotos RESPONSE " + response)
            if (response.isSuccessful)
                response.body().photos
            else null
        }

//        lastPhotosRequest = mainObserver.cache()
//        return lastPhotosRequest
//        return mainObserver
    }


    fun getRovers(): Observable<Rover> {
        return Observable
                .fromIterable(localRovers())
                .flatMap {
                    // Merge for local and for inet rovers.
                    Observable.mergeDelayError(
                            // Just return local rover
                            Observable.just(it),
                            // Load rover from Inet
                            Observable.fromCallable {
                                val callResponse = api.getRoverInfo(it.name)
                                val response = callResponse.execute()
                                if (response.isSuccessful) {
                                    Log.w("Sirelon", "Response is " + response.body().roverInfo)
                                    val roverResponse = response.body().roverInfo
                                    val newRover = it.copy()

                                    newRover.landingDate = roverResponse.landingDate
                                    newRover.launchDate = roverResponse.launchDate
                                    newRover.maxDate = roverResponse.maxDate
                                    newRover.maxSol = roverResponse.maxSol
                                    newRover.totalPhotos = roverResponse.totalPhotos
                                    newRover.status = roverResponse.status
                                    newRover
                                } else
                                    null
                            }.subscribeOn(Schedulers.newThread())
                    )
                }
    }

    private fun localRovers(): List<Rover> = roverRepo.getAllRovers()

    val roverRepo by lazy {
        RoversRepository(context)
    }
}
