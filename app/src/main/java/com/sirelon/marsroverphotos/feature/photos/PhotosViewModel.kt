package com.sirelon.marsroverphotos.feature.photos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sirelon.marsroverphotos.models.PhotosQueryRequest
import com.sirelon.marsroverphotos.network.RestApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Created on 21.02.2021 20:25 for Mars-Rover-Photos.
 */
class PhotosViewModel(app: Application) : AndroidViewModel(app) {

    private val restApi = RestApi(app)
    private val photosRepository = PhotosRepository(restApi)

    private val queryEmmiter = MutableSharedFlow<PhotosQueryRequest>()

    val photosFlow = queryEmmiter
        .distinctUntilChanged()
        .map { photosRepository.refreshImages(it) }
        .catch { it.printStackTrace() }
        .flowOn(Dispatchers.IO)

    fun setPhotosQuery(query: PhotosQueryRequest) {
        viewModelScope.launch {
            queryEmmiter.emit(query)
        }
    }

}