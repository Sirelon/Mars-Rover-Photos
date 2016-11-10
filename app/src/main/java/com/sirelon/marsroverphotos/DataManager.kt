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

    fun getMarsPhotos(queryRequest: PhotosQueryRequest): Observable<MutableList<MarsPhoto>> {
        return Observable.fromCallable {
            val callResponse = api.getRoversPhotos(queryRequest)
            val response = callResponse.execute()
            response.body().photos
        }
    }

    fun getRovers(): Observable<Rover> {
        return Observable.create({
            subscription ->
            Observable
                    .fromIterable(localRovers())
                    // Return local rover to subscription
                    .map {
                        subscription.onNext(it)
                        it
                    }
                    .observeOn(Schedulers.io())
                    // Execute API for getting info for rover, and call onNext for it
                    .map {
                        val callResponse = api.getRoverInfo(it.name)
                        val response = callResponse.execute()
                        Log.w("Sirelon", "Response is " + response.body().roverInfo)
                        if (response.isSuccessful) {
                            val roverResponse = response.body().roverInfo
                            val newRover = it.copy()

                            newRover.landingDate = roverResponse.landingDate
                            newRover.launchDate = roverResponse.launchDate
                            newRover.maxDate = roverResponse.maxDate
                            newRover.maxSol = roverResponse.maxSol
                            newRover.totalPhotos = roverResponse.totalPhotos
                            newRover.status = roverResponse.status

                            roverRepo.saveRover(newRover)

                            subscription.onNext(newRover)
                        }
                    }
                    .subscribe({}, { subscription.onError(it) }, { subscription.onComplete() })
        })
    }

    private fun localRovers(): List<Rover> = roverRepo.getAllRovers()

    val roverRepo by lazy {
        RoversRepository(context)
    }
}
