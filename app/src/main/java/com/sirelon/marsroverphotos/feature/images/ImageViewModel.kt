package com.sirelon.marsroverphotos.feature.images

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.sirelon.marsroverphotos.RoverApplication
import com.sirelon.marsroverphotos.storage.MarsImage

/**
 * Created on 22.08.2020 18:59 for Mars-Rover-Photos.
 */
class ImageViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ImagesRepository(app)

    private val idsEmitor = MutableLiveData<List<Int>>()

    val imagesLiveData = idsEmitor.switchMap { repository.loadImages(it) }

    fun setIdsToShow(ids: List<Int>) {
        idsEmitor.value = ids
    }

    fun updateFavorite(image: MarsImage) {
        repository.updateFavForImage(image)

        val tracker = getApplication<RoverApplication>().tracker
        tracker.trackFavorite(image.toMarsPhoto(), "Images", !image.favorite)
    }
}