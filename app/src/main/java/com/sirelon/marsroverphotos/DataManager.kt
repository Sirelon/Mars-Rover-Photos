package com.sirelon.marsroverphotos

import android.annotation.SuppressLint
import android.content.Context
import com.sirelon.marsroverphotos.feature.images.ImagesRepository
import com.sirelon.marsroverphotos.feature.photos.PhotosRepository
import com.sirelon.marsroverphotos.feature.rovers.InsightId
import com.sirelon.marsroverphotos.feature.rovers.RoversRepository
import com.sirelon.marsroverphotos.firebase.photos.FirebaseProvider.firebasePhotos
import com.sirelon.marsroverphotos.network.RestApi
import com.sirelon.marsroverphotos.storage.MarsImage
import com.sirelon.marsroverphotos.tracker.ITracker
import com.sirelon.marsroverphotos.tracker.normalizeClickEventName
import com.sirelon.marsroverphotos.tracker.normalizeEventName
import com.sirelon.marsroverphotos.tracker.toAnalyticsBundle
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * @author romanishin
 * @since 31.10.16 on 12:43
 */
@SuppressLint("CheckResult")
class DataManager(
    val context: Context, private val tracker: ITracker,
     api: RestApi = RestApi()
) {

    val roverRepo = RoversRepository(context, api)
    val imagesRepo = ImagesRepository(context)
    val photosRepo = PhotosRepository(api)

    val rovers = roverRepo.getRovers()

    private val exceptionHandler =
        CoroutineExceptionHandler { _, throwable -> throwable.printStackTrace() }

    init {
        GlobalScope.launch(exceptionHandler) {
            val count = firebasePhotos.countOfInsightPhotos()
            roverRepo.updateRoverCountPhotos(InsightId, count)
        }
    }

    suspend fun updatePhotoSeenCounter(marsPhoto: MarsImage) {
        firebasePhotos.updatePhotoSeenCounter(marsPhoto)
        tracker.trackSeen(marsPhoto)
    }

    suspend fun updatePhotoScaleCounter(marsPhoto: MarsImage) {
        firebasePhotos.updatePhotoScaleCounter(marsPhoto)
        tracker.trackScale(marsPhoto)
    }

    suspend fun updatePhotoSaveCounter(image: MarsImage) {
        firebasePhotos.updatePhotoSaveCounter(image)
        tracker.trackSave(image)
    }

    suspend fun updatePhotoShareCounter(marsPhoto: MarsImage, packageName: String?) {
        firebasePhotos.updatePhotoShareCounter(marsPhoto)
        tracker.trackShare(marsPhoto, packageName ?: "Not Specified")
    }

    fun trackClick(event: String) {
        tracker.trackClick(normalizeClickEventName(event))
    }

    fun trackEvent(event: String, params: Map<String, Any?> = emptyMap()) {
        val normalizedEvent = normalizeEventName(event)
        Timber.d("trackEvent() called with: event = $event, normalized = $normalizedEvent, params = $params")
        tracker.trackEvent(normalizedEvent, params.toAnalyticsBundle())
    }

    suspend fun cacheImages(photos: List<MarsImage>) {
        imagesRepo.saveImages(photos)
    }
}
