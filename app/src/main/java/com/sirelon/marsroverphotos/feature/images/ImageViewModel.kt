package com.sirelon.marsroverphotos.feature.images

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.sirelon.marsroverphotos.RoverApplication
import com.sirelon.marsroverphotos.storage.MarsImage
import kotlinx.coroutines.launch

/**
 * Created on 22.08.2020 18:59 for Mars-Rover-Photos.
 */
class ImageViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ImagesRepository(app)

    private val idsEmitor = MutableLiveData<List<String>>()

    val imagesLiveData = idsEmitor.switchMap { repository.loadImages(it) }

    fun setIdsToShow(ids: List<String>) {
        idsEmitor.value = ids
    }

    fun updateFavorite(image: MarsImage) {
        viewModelScope.launch {
            kotlin.runCatching { repository.updateFavForImage(image) }
        }

        val tracker = getApplication<RoverApplication>().tracker
        tracker.trackFavorite(image.toMarsPhoto(), "Images", !image.favorite)
    }
}