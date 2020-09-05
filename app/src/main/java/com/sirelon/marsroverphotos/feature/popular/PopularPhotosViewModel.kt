package com.sirelon.marsroverphotos.feature.popular

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.sirelon.marsroverphotos.feature.images.ImagesRepository

/**
 * Created on 31.08.2020 21:59 for Mars-Rover-Photos.
 */
class PopularPhotosViewModel(app: Application) : AndroidViewModel(app) {

    private val imagesRepository = ImagesRepository(app)

    val popularPhotos = imagesRepository.loadPopularPagedSource().cachedIn(viewModelScope)


}