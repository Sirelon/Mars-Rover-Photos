package com.sirelon.marsroverphotos.feature.favorite

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.sirelon.marsroverphotos.feature.images.ImagesRepository
import com.sirelon.marsroverphotos.storage.DataBaseProvider
import com.sirelon.marsroverphotos.storage.ImagesDao
import com.sirelon.marsroverphotos.storage.MarsImage

/**
 * Created on 25.08.2020 12:13 for Mars-Rover-Photos.
 */
class FavoriteImagesViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ImagesRepository(app)

    val favoriteImagesFlow = repository.loadPagedSource()

    fun updateFavForImage(image: MarsImage) {
        repository.updateFavForImage(item = image)
    }
}