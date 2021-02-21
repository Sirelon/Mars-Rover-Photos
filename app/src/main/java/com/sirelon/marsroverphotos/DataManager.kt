package com.sirelon.marsroverphotos

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.LiveData
import com.sirelon.marsroverphotos.feature.images.ImagesRepository
import com.sirelon.marsroverphotos.feature.rovers.INSIGHT_ID
import com.sirelon.marsroverphotos.feature.rovers.RoversRepository
import com.sirelon.marsroverphotos.firebase.photos.FirebaseProvider.firebasePhotos
import com.sirelon.marsroverphotos.models.MarsPhoto
import com.sirelon.marsroverphotos.models.PhotosQueryRequest
import com.sirelon.marsroverphotos.network.RestApi
import com.sirelon.marsroverphotos.storage.MarsImage
import com.sirelon.marsroverphotos.tracker.ITracker
import io.reactivex.Observable
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

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

    private val roverRepo = RoversRepository(context, api)
    private val imagesRepo = ImagesRepository(context)

    val rovers = roverRepo.getRovers()

    private val exceptionHandler =
        CoroutineExceptionHandler { _, throwable -> throwable.printStackTrace() }

    init {
        GlobalScope.launch(exceptionHandler) {
            val count = firebasePhotos.countOfInsightPhotos()
            roverRepo.updateRoverCountPhotos(INSIGHT_ID, count)
        }
    }

//    fun loadFirstFavoriteItem(): LiveData<MarsImage?> = imagesRepo.loadFirstImage()

//    fun loadMarsPhotos(queryRequest: PhotosQueryRequest): Observable<List<MarsImage>?> {
//        api.getRoversPhotos(queryRequest)

//        val mainObservable = Observable.fromCallable {
//            val callResponse = api.getRoversPhotos(queryRequest)
//            val response = callResponse.execute()
//            if (response.isSuccessful) response.body()?.photos
//            else throw RuntimeException(response.errorBody()?.string())
//        }
//
//        lastPhotosRequest = mainObservable.cache()
//
//        return lastPhotosRequest!!
//    }

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

    suspend fun cacheImages(photos: List<MarsImage>) {
        imagesRepo.saveImages(photos)
    }
}
