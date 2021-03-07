package com.sirelon.marsroverphotos.feature.photos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sirelon.marsroverphotos.extensions.logD
import com.sirelon.marsroverphotos.feature.rovers.PERSEVARANCE_ID
import com.sirelon.marsroverphotos.feature.rovers.RoversRepository
import com.sirelon.marsroverphotos.models.PhotosQueryRequest
import com.sirelon.marsroverphotos.network.RestApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Created on 21.02.2021 20:25 for Mars-Rover-Photos.
 */
class PhotosViewModel(app: Application) : AndroidViewModel(app) {

    private val restApi = RestApi(app)
    private val roversRepository = RoversRepository(app, restApi)
    private val photosRepository = PhotosRepository(restApi)

    private val queryEmmiter = MutableStateFlow<PhotosQueryRequest?>(null)
    private val roverIdEmmiter = MutableStateFlow<Long?>(null)

    val roverFlow = roverIdEmmiter
        .filterNotNull()
        .map { roversRepository.loadRoverById(it) }
        .catch { it.printStackTrace() }
        .flowOn(Dispatchers.IO)

    val photosFlow = queryEmmiter
        .filterNotNull()
        .map { photosRepository.refreshImages(it) }
        .catch { it.printStackTrace() }
        .flowOn(Dispatchers.IO)


    init {
        viewModelScope.launch(Dispatchers.IO) {
            roverFlow.first()
            randomize()
        }
    }


    fun setPhotosQuery(query: PhotosQueryRequest) {
        query.logD()
        viewModelScope.launch {
            queryEmmiter.emit(query)
        }
    }

    fun setRoverId(roverId: Long) {
        roverIdEmmiter.tryEmit(roverId)
    }

    fun randomize() {
        viewModelScope.launch(Dispatchers.IO) {
            val query = randomPhotosQueryRequest()
            setPhotosQuery(query)
        }
    }

    fun getSol() = queryEmmiter.value?.sol ?: 0

    private suspend fun randomPhotosQueryRequest(): PhotosQueryRequest {
        val rover = roverFlow.first()
        val sol = Random.nextLong(0L, rover?.maxSol ?: 10)
        return PhotosQueryRequest(rover?.id ?: PERSEVARANCE_ID, sol, null)
    }

    fun loadBySol(sol: Long) {
        val queryRequest = queryEmmiter.value ?: return
        if (sol == queryRequest.sol) return

        val queryUpdated = queryRequest.copy(sol = sol)
        setPhotosQuery(queryUpdated)

        //        // Clear adapter
//        adapter.clearAll()
//        loadFreshData()
    }
}
