package com.sirelon.marsroverphotos.feature.photos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sirelon.marsroverphotos.RoverApplication
import com.sirelon.marsroverphotos.extensions.logD
import com.sirelon.marsroverphotos.feature.images.ImagesRepository
import com.sirelon.marsroverphotos.feature.rovers.RoversRepository
import com.sirelon.marsroverphotos.models.PhotosQueryRequest
import com.sirelon.marsroverphotos.models.RoverDateUtil
import com.sirelon.marsroverphotos.network.RestApi
import com.sirelon.marsroverphotos.storage.MarsImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Created on 21.02.2021 20:25 for Mars-Rover-Photos.
 */
class PhotosViewModel(app: Application) : AndroidViewModel(app) {

    private val restApi = RestApi(app)
    private val roversRepository = RoversRepository(app, restApi)
    private val photosRepository = PhotosRepository(restApi)
    private val imagesRepository = ImagesRepository(app)

    private val queryEmmiter = MutableStateFlow<PhotosQueryRequest?>(null)
    private val roverIdEmmiter = MutableStateFlow<Long?>(null)
    private var dateUtil: RoverDateUtil? = null

    private val roverFlow = roverIdEmmiter
        .filterNotNull()
        .mapNotNull { roversRepository.loadRoverById(it) }
        .onEach { dateUtil = RoverDateUtil(it) }
        .catch { it.printStackTrace() }
        .flowOn(Dispatchers.IO)

    // null means that we are in loading process.
    val photosFlow = queryEmmiter
        .map {
            if (it != null) photosRepository.refreshImages(it)
            else null
        }
        .catch { it.printStackTrace() }
        .flowOn(Dispatchers.IO)

    val solFlow = queryEmmiter.mapNotNull { it?.sol }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            roverFlow.first()
            randomize()
        }
    }


    fun setPhotosQuery(query: PhotosQueryRequest) {
        query.logD()
        queryEmmiter.tryEmit(null)
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

    fun earthDateStr(sol: Long): String {
        val time = earthTime(sol)
        return dateUtil?.parseTime(time) ?: ""
    }

    fun earthTime(sol: Long = getSol()) = dateUtil?.dateFromSol(sol) ?: System.currentTimeMillis()

    fun setEarthTime(time: Long) {
        val sol = dateUtil?.solFromDate(time) ?: 1
        loadBySol(sol)
    }

    fun maxDate() = dateUtil?.roverLastDate ?: System.currentTimeMillis()

    suspend fun maxSol() = roverFlow.first().maxSol

    fun minDate() = dateUtil?.roverLandingDate ?: System.currentTimeMillis()

    fun dateFromSol() = dateUtil?.dateFromSol(getSol()) ?: System.currentTimeMillis()

    private suspend fun randomPhotosQueryRequest(): PhotosQueryRequest {
        val rover = roverFlow.first()
        val sol = Random.nextLong(0L, rover.maxSol)
        return PhotosQueryRequest(rover.id, sol, null)
    }

    fun loadBySol(sol: Long) {
        val queryRequest = queryEmmiter.value ?: return
        if (sol == queryRequest.sol) return
        queryEmmiter.tryEmit(null)

        val queryUpdated = queryRequest.copy(sol = sol)
        setPhotosQuery(queryUpdated)
    }

    fun onPhotoClick(image: MarsImage) {
        viewModelScope.launch(Dispatchers.IO) {
            val photos = photosFlow.filterNot { it.isNullOrEmpty() }.first()
            photos.logD()
            photos ?: return@launch
            imagesRepository.saveImages(photos)
        }

    }

    fun track(track: String) {
        RoverApplication.APP.dataManger.trackClick(track)
    }
}
