package com.sirelon.marsroverphotos.feature.popular

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.sirelon.marsroverphotos.RoverApplication
import com.sirelon.marsroverphotos.feature.images.ImagesRepository
import com.sirelon.marsroverphotos.storage.MarsImage
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Created on 31.08.2020 21:59 for Mars-Rover-Photos.
 */
class PopularPhotosViewModel(app: Application) : AndroidViewModel(app) {
    private val imagesRepository = ImagesRepository(app)

    val popularPhotos = imagesRepository.loadPopularPagedSource().map {
        Log.d("Sirelon", "MAP $it")
        it
    }

    fun loadPopular() = imagesRepository.loadPopularPhotos()

    fun updateFavorite(image: MarsImage) {
        viewModelScope.launch {
            kotlin.runCatching { imagesRepository.updateFavForImage(image) }
        }

        val tracker = getApplication<RoverApplication>().tracker
        tracker.trackFavorite(image.toMarsPhoto(), "PopularList", !image.favorite)
    }
}