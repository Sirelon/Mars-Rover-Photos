package com.sirelon.marsroverphotos.feature.images

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.sirelon.marsroverphotos.RoverApplication
import com.sirelon.marsroverphotos.extensions.recordException
import com.sirelon.marsroverphotos.firebase.photos.FirebaseProvider
import com.sirelon.marsroverphotos.models.MarsPhoto
import com.sirelon.marsroverphotos.storage.MarsImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * Created on 22.08.2020 18:59 for Mars-Rover-Photos.
 */
class ImageViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ImagesRepository(app)

    private val idsEmitor = MutableStateFlow<List<String>>(emptyList())

    val imagesLiveData = idsEmitor
        .flatMapLatest { repository.loadImages(it) }
        .flatMapLatest {
            if (it.isEmpty()) {
                recordException(IllegalStateException("Room is empty"))
                delay(500)
                repository.loadImages(idsEmitor.value)
            } else {
                flowOf(it)
            }
        }
        .asLiveData()

    fun setIdsToShow(ids: List<String>) {
        idsEmitor.value = ids
    }

    fun updateFavorite(image: MarsImage) {
        viewModelScope.launch {
            kotlin.runCatching { repository.updateFavForImage(image) }
                .onFailure {
                    it.printStackTrace()
                }
        }

        val tracker = getApplication<RoverApplication>().tracker
        tracker.trackFavorite(image.toMarsPhoto(), "Images", !image.favorite)
    }


    fun updatePhotoScaleCounter(marsPhoto: MarsPhoto?) {
        marsPhoto?.let {
            FirebaseProvider.firebasePhotos.updatePhotoScaleCounter(marsPhoto)
                .onErrorReturn { 0 }
                .subscribe()
            val tracker = getApplication<RoverApplication>().tracker
            tracker.trackScale(marsPhoto)
        }
    }
}