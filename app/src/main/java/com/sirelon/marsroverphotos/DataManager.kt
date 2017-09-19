package com.sirelon.marsroverphotos

import android.content.Context
import com.sirelon.marsroverphotos.firebase.firebasePhotos
import com.sirelon.marsroverphotos.models.MarsPhoto
import com.sirelon.marsroverphotos.models.PhotosQueryRequest
import com.sirelon.marsroverphotos.models.Rover
import com.sirelon.marsroverphotos.network.RestApi
import com.sirelon.marsroverphotos.tracker.ITracker
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

/**
 * @author romanishin
 * @since 31.10.16 on 12:43
 */
class DataManager(val context: Context, private val tracker: ITracker, private val api: RestApi = RestApi(context)) {

    /**
     * This var if you want to reuse the mainObservable without making new request on server
     */
    var lastPhotosRequest: Observable<MutableList<MarsPhoto>?>? = null

    fun loadMarsPhotos(queryRequest: PhotosQueryRequest): Observable<MutableList<MarsPhoto>?> {
        val mainObservable = Observable.fromCallable {
            val callResponse = api.getRoversPhotos(queryRequest)
            val response = callResponse.execute()
            if (response.isSuccessful)
                response.body()?.photos
            else throw RuntimeException(response.errorBody()?.string())
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
                                    val newRover = it.copy()

                                    val roverResponse = response.body()?.roverInfo

                                    if (roverResponse != null) {
                                        newRover.apply {
                                            landingDate = roverResponse.landingDate
                                            launchDate = roverResponse.launchDate
                                            maxDate = roverResponse.maxDate
                                            maxSol = roverResponse.maxSol
                                            totalPhotos = roverResponse.totalPhotos
                                            status = roverResponse.status
                                        }
                                    }

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

    private val roverRepo by lazy {
        RoversRepository(context)
    }

    fun updatePhotoSeenCounter(marsPhoto: MarsPhoto?) {
        marsPhoto?.let {
            firebasePhotos.updatePhotoSeenCounter(marsPhoto).onErrorReturn { 0 }.subscribe()
            tracker.trackSeen(marsPhoto)
        }
    }

    fun updatePhotoScaleCounter(marsPhoto: MarsPhoto?) {
        marsPhoto?.let {
            firebasePhotos.updatePhotoScaleCounter(marsPhoto).onErrorReturn { 0 }.subscribe()
            tracker.trackScale(marsPhoto)
        }
    }

    fun updatePhotoSaveCounter(marsPhoto: MarsPhoto?) {
        marsPhoto?.let {
            firebasePhotos.updatePhotoSaveCounter(marsPhoto).onErrorReturn { 0 }.subscribe()
            tracker.trackSave(marsPhoto)
        }
    }

    fun updatePhotoShareCounter(marsPhoto: MarsPhoto?, packageName: String) {
        marsPhoto?.let {
            firebasePhotos.updatePhotoShareCounter(marsPhoto).onErrorReturn { 0 }.subscribe()
            tracker.trackShare(marsPhoto, packageName)
        }
    }
}
