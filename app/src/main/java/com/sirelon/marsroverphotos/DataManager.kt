package com.sirelon.marsroverphotos

import android.content.Context
import com.sirelon.marsroverphotos.extensions.logD
import com.sirelon.marsroverphotos.models.MarsPhoto
import com.sirelon.marsroverphotos.models.PhotosQueryRequest
import com.sirelon.marsroverphotos.models.Rover
import com.sirelon.marsroverphotos.network.RestApi
import com.sirelon.marsroverphotos.network.firebasePhotos
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

/**
 * @author romanishin
 * @since 31.10.16 on 12:43
 */
class DataManager(val context: Context, private val api: RestApi = RestApi()) {

    /**
     * This var if you want to reuse the mainObservable without making new request on server
     */
    var lastPhotosRequest: Observable<MutableList<MarsPhoto>?>? = null

    fun loadMarsPhotos(queryRequest: PhotosQueryRequest): Observable<MutableList<MarsPhoto>?> {
        val mainObservable = Observable.fromCallable {
            val callResponse = api.getRoversPhotos(queryRequest)
            val response = callResponse.execute()
            if (response.isSuccessful)
                response.body().photos
            else throw RuntimeException(response.errorBody().string())
        }

        lastPhotosRequest = mainObservable.cache()

        return lastPhotosRequest!!
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
                            }
                                    .onErrorReturnItem(it)
                                    .subscribeOn(Schedulers.newThread())
                    )
                }
    }

    private fun localRovers(): List<Rover> = roverRepo.getAllRovers()

    val roverRepo by lazy {
        RoversRepository(context)
    }

    fun updatePhotoSeenCounter(marsPhoto: MarsPhoto?) {
        marsPhoto?.let {
            firebasePhotos.updatePhotoSeenCounter(marsPhoto).subscribe(Long::logD)
        }
    }

    fun updatePhotoScaleCounter(marsPhoto: MarsPhoto?) {
        marsPhoto?.let {
            firebasePhotos.updatePhotoScaleCounter(marsPhoto).subscribe(Long::logD)
        }
    }

    fun  updatePhotoSaveCounter(marsPhoto: MarsPhoto?) {
        marsPhoto?.let {
            firebasePhotos.updatePhotoSaveCounter(marsPhoto).subscribe(Long::logD)
        }
    }

    fun  updatePhotoShareCounter(marsPhoto: MarsPhoto?) {
        marsPhoto?.let {
            firebasePhotos.updatePhotoShareCounter(marsPhoto).subscribe(Long::logD)
        }
    }
}
