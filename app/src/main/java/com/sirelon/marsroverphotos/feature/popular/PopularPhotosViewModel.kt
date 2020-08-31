package com.sirelon.marsroverphotos.feature.popular

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.map
import com.sirelon.marsroverphotos.extensions.logD
import com.sirelon.marsroverphotos.firebase.photos.FirebaseProvider
import com.sirelon.marsroverphotos.storage.DataBaseProvider
import com.sirelon.marsroverphotos.storage.ImagesDao
import com.sirelon.marsroverphotos.storage.MarsImage
import kotlinx.coroutines.flow.map

/**
 * Created on 31.08.2020 21:59 for Mars-Rover-Photos.
 */
class PopularPhotosViewModel(app: Application) : AndroidViewModel(app) {

    val popularPhotos = Pager(
        config = PagingConfig(10, 2, true),
        pagingSourceFactory = {
            PopularPhotosSource(FirebaseProvider.firebasePhotos)
        })
        .flow
        .map { data ->
            data.map {
                it.logD()
                MarsImage(
                    it.id.toInt(),
                    it.sol,
                    it.name,
                    it.imageUrl,
                    it.earthDate,
                    null,
                    false
                )
            }
        }
        .cachedIn(viewModelScope)


}