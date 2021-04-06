package com.sirelon.marsroverphotos.feature.favorite

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sirelon.marsroverphotos.RoverApplication
import com.sirelon.marsroverphotos.feature.images.ImagesRepository
import com.sirelon.marsroverphotos.storage.MarsImage
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Created on 25.08.2020 12:13 for Mars-Rover-Photos.
 */
class FavoriteImagesViewModel(app: Application) : AndroidViewModel(app) {

    private val exceptionHandler =
        CoroutineExceptionHandler { _, throwable -> throwable.printStackTrace() }

    private val repository = ImagesRepository(app)

    val favoriteImagesFlow = repository.loadFavoritePagedSource()

    fun updateFavForImage(image: MarsImage) {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            repository.updateFavForImage(item = image)
        }

        val tracker = getApplication<RoverApplication>().tracker
        tracker.trackFavorite(image.toMarsPhoto(), "FavoriteList", !image.favorite)
    }

    fun track(track: String) {
        RoverApplication.APP.dataManger.trackClick(track)
    }
}