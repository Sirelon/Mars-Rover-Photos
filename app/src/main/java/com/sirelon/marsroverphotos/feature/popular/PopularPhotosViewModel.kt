package com.sirelon.marsroverphotos.feature.popular

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.sirelon.marsroverphotos.RoverApplication
import com.sirelon.marsroverphotos.extensions.exceptionHandler
import com.sirelon.marsroverphotos.extensions.logD
import com.sirelon.marsroverphotos.extensions.recordException
import com.sirelon.marsroverphotos.feature.images.ImagesRepository
import com.sirelon.marsroverphotos.storage.MarsImage
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Created on 31.08.2020 21:59 for Mars-Rover-Photos.
 */
class PopularPhotosViewModel(app: Application) : AndroidViewModel(app) {
    private val imagesRepository = ImagesRepository(app)

    init {
        logD()
    }

    val popularPhotos = imagesRepository.loadPopularPagedSource()

    fun loadPopular() = imagesRepository.loadPopularPhotos()

    fun updateFavorite(image: MarsImage) {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            imagesRepository.updateFavForImage(image)
        }

        val tracker = getApplication<RoverApplication>().tracker
        tracker.trackFavorite(image.toMarsPhoto(), "PopularList", !image.favorite)
    }
}