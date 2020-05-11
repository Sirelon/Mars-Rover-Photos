package com.sirelon.marsroverphotos

import android.annotation.SuppressLint
import android.content.Context
import com.sirelon.marsroverphotos.extensions.logE
import com.sirelon.marsroverphotos.feature.rovers.INSIGHT_ID
import com.sirelon.marsroverphotos.feature.rovers.RoversRepository
import com.sirelon.marsroverphotos.firebase.photos.FirebaseProvider.firebasePhotos
import com.sirelon.marsroverphotos.models.MarsPhoto
import com.sirelon.marsroverphotos.models.PhotosQueryRequest
import com.sirelon.marsroverphotos.network.RestApi
import com.sirelon.marsroverphotos.tracker.ITracker
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

/**
 * @author romanishin
 * @since 31.10.16 on 12:43
 */
@SuppressLint("CheckResult")
class DataManager(
    val context: Context, private val tracker: ITracker,
    private val api: RestApi = RestApi(context)
) {

    /**
     * This var if you want to reuse the mainObservable without making new request on server
     */
    var lastPhotosRequest: Observable<List<MarsPhoto>?>? = null

    private val roverRepo = RoversRepository(context)

    val rovers = roverRepo.getRovers()

    init {
        firebasePhotos.countOfInsightPhotos()
            .flatMapCompletable { roverRepo.updateRoverCountPhotos(INSIGHT_ID, it) }
            .onErrorComplete()
            .subscribe()

        Observable.fromArray("curiosity", "opportunity", "spirit")
            .map(api::getRoverInfo)
            .toList()
            .subscribeOn(Schedulers.io())
            .flatMapObservable { Observable.merge(it) }
            .toList()
            .subscribe(roverRepo::updateRoversByInfo, Throwable::logE)
    }

    fun loadMarsPhotos(queryRequest: PhotosQueryRequest): Observable<List<MarsPhoto>?> {
        val mainObservable = Observable.fromCallable {
            val callResponse = api.getRoversPhotos(queryRequest)
            val response = callResponse.execute()
            if (response.isSuccessful) response.body()?.photos
            else throw RuntimeException(response.errorBody()?.string())
        }

        lastPhotosRequest = mainObservable.cache()

        return lastPhotosRequest!!
    }

    fun updatePhotoSeenCounter(marsPhoto: MarsPhoto?) {
        marsPhoto?.let {
            firebasePhotos.updatePhotoSeenCounter(marsPhoto)
                .onErrorReturn { 0 }
                .subscribe()
            tracker.trackSeen(marsPhoto)
        }
    }

    fun updatePhotoScaleCounter(marsPhoto: MarsPhoto?) {
        marsPhoto?.let {
            firebasePhotos.updatePhotoScaleCounter(marsPhoto)
                .onErrorReturn { 0 }
                .subscribe()
            tracker.trackScale(marsPhoto)
        }
    }

    fun updatePhotoSaveCounter(marsPhoto: MarsPhoto?) {
        marsPhoto?.let {
            firebasePhotos.updatePhotoSaveCounter(marsPhoto)
                .onErrorReturn { 0 }
                .subscribe()
            tracker.trackSave(marsPhoto)
        }
    }

    fun updatePhotoShareCounter(marsPhoto: MarsPhoto?, packageName: String?) {
        marsPhoto?.let {
            firebasePhotos.updatePhotoShareCounter(marsPhoto)
                .onErrorReturn { 0 }
                .subscribe()
            tracker.trackShare(marsPhoto, packageName ?: "Not Specified")
        }
    }

    fun trackClick(event: String) {
        tracker.trackClick("click_$event")
    }
}
