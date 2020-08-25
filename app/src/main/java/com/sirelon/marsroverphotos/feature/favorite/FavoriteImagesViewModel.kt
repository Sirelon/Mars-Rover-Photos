package com.sirelon.marsroverphotos.feature.favorite

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.sirelon.marsroverphotos.storage.DataBaseProvider
import com.sirelon.marsroverphotos.storage.ImagesDao

/**
 * Created on 25.08.2020 12:13 for Mars-Rover-Photos.
 */
class FavoriteImagesViewModel(app: Application) : AndroidViewModel(app) {

    private val dao: ImagesDao

    init {
        DataBaseProvider.init(app)
        dao = DataBaseProvider.dataBase.imagesDao()
    }


    val favoriteImages = dao.getFavoriteImages()


}