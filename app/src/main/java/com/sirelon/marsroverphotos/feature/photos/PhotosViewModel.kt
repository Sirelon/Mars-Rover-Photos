package com.sirelon.marsroverphotos.feature.photos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sirelon.marsroverphotos.RoverApplication
import com.sirelon.marsroverphotos.extensions.exceptionHandler
import com.sirelon.marsroverphotos.extensions.logD
import com.sirelon.marsroverphotos.extensions.recordException
import com.sirelon.marsroverphotos.models.PhotosQueryRequest
import com.sirelon.marsroverphotos.models.RoverDateUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
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

    private val dataManger = getApplication<RoverApplication>().dataManger
    private val roversRepository = dataManger.roverRepo
    private val photosRepository = dataManger.photosRepo
    private val imagesRepository = dataManger.imagesRepo

    private val queryEmitter = MutableStateFlow<PhotosQueryRequest?>(null)
    private val roverIdEmitter = MutableStateFlow<Long?>(null)
    private var dateUtil: RoverDateUtil? = null

    private val roverFlow = roverIdEmitter
        .filterNotNull()
        .mapNotNull { roversRepository.loadRoverById(it) }
        .onEach { dateUtil = RoverDateUtil(it) }
        .catch { recordException(it) }
        .flowOn(Dispatchers.IO)

    // null means that we are in loading process.
    val photosFlow = queryEmitter
        .map {
            if (it != null) photosRepository.refreshImages(it)
            else null
        }
        .catch { recordException(it) }
        .flowOn(Dispatchers.IO)

    val solFlow = queryEmitter.mapNotNull { it?.sol }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            roverFlow.first()
            randomize()
        }
    }

    private fun setPhotosQuery(query: PhotosQueryRequest) {
        query.logD()
        queryEmitter.tryEmit(null)
        viewModelScope.launch {
            queryEmitter.emit(query)
        }
    }

    fun setRoverId(roverId: Long) {
        roverIdEmitter.tryEmit(roverId)
    }

    fun randomize() {
        viewModelScope.launch(Dispatchers.IO) {
            val query = randomPhotosQueryRequest()
            setPhotosQuery(query)
        }
    }

    fun goToLatest() {
        viewModelScope.launch(Dispatchers.IO) {
            val rover = roverFlow.first()
            val maxSol = rover.maxSol - 1
            val query = if (queryEmitter.value?.sol == maxSol) {
                randomPhotosQueryRequest()
            } else {
                PhotosQueryRequest(rover.id, maxSol, null)
            }

            setPhotosQuery(query)
        }
    }

    private fun getSol() = queryEmitter.value?.sol ?: 0

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
        val queryRequest = queryEmitter.value ?: return
        if (sol == queryRequest.sol) return
        queryEmitter.tryEmit(null)

        val queryUpdated = queryRequest.copy(sol = sol)
        setPhotosQuery(queryUpdated)
    }

    fun onPhotoClick() {
        GlobalScope.launch(Dispatchers.IO + exceptionHandler) {
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
