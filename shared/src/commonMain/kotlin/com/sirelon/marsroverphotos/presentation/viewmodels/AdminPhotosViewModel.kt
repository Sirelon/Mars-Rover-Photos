package com.sirelon.marsroverphotos.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirelon.marsroverphotos.domain.models.FirebasePhoto
import com.sirelon.marsroverphotos.platform.IFirebasePhotos
import com.sirelon.marsroverphotos.platform.createHttpClientEngine
import com.sirelon.marsroverphotos.utils.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.head
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

enum class PhotoCheckStatus { Pending, Checking, Ok, Stale, Error }

data class AdminPhotoItem(
    val photo: FirebasePhoto,
    val status: PhotoCheckStatus = PhotoCheckStatus.Pending,
    val selected: Boolean = false
)

data class AdminPhotosState(
    val phase: Phase = Phase.Idle,
    val photos: List<AdminPhotoItem> = emptyList(),
    val isDeleting: Boolean = false,
    val deletedCount: Int = 0
) {
    enum class Phase { Idle, FetchingPhotos, CheckingUrls, Done }

    val checkedCount: Int get() = photos.count {
        it.status != PhotoCheckStatus.Pending && it.status != PhotoCheckStatus.Checking
    }
    val stalePhotos: List<AdminPhotoItem> get() = photos.filter {
        it.status == PhotoCheckStatus.Stale || it.status == PhotoCheckStatus.Error
    }
    val selectedPhotos: List<AdminPhotoItem> get() = photos.filter { it.selected }
}

class AdminPhotosViewModel(
    private val firebasePhotos: IFirebasePhotos
) : ViewModel() {

    private val httpClient = HttpClient(createHttpClientEngine()) {
        expectSuccess = false
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 15_000
        }
    }

    private val _state = MutableStateFlow(AdminPhotosState())
    val state: StateFlow<AdminPhotosState> = _state.asStateFlow()

    override fun onCleared() {
        super.onCleared()
        httpClient.close()
    }

    fun startCheck() {
        viewModelScope.launch {
            fetchAllPhotos()
            checkAllUrls()
        }
    }

    private suspend fun fetchAllPhotos() {
        _state.update { it.copy(phase = AdminPhotosState.Phase.FetchingPhotos, photos = emptyList()) }
        val allPhotos = mutableListOf<FirebasePhoto>()
        var lastId: String? = null
        while (true) {
            val page = try {
                firebasePhotos.loadPopularPhotos(count = 100, lastPhotoId = lastId)
            } catch (e: Exception) {
                Logger.e("AdminPhotosViewModel", e) { "Failed to fetch photos page" }
                break
            }
            if (page.isEmpty()) break
            allPhotos.addAll(page)
            lastId = page.last().id
        }
        _state.update { it.copy(photos = allPhotos.map { photo -> AdminPhotoItem(photo) }) }
    }

    private suspend fun checkAllUrls() {
        _state.update { it.copy(phase = AdminPhotosState.Phase.CheckingUrls) }
        val photos = _state.value.photos
        val semaphore = Semaphore(20)
        val jobs = photos.map { item ->
            viewModelScope.async {
                semaphore.withPermit { checkUrl(item.photo.id, item.photo.imageUrl) }
            }
        }
        jobs.awaitAll()
        // Auto-select all stale after check completes
        _state.update { state ->
            state.copy(
                phase = AdminPhotosState.Phase.Done,
                photos = state.photos.map { item ->
                    if (item.status == PhotoCheckStatus.Stale || item.status == PhotoCheckStatus.Error) {
                        item.copy(selected = true)
                    } else item
                }
            )
        }
    }

    private suspend fun checkUrl(id: String, url: String) {
        if (url.isBlank()) {
            updatePhotoStatus(id, PhotoCheckStatus.Stale)
            return
        }
        updatePhotoStatus(id, PhotoCheckStatus.Checking)
        val status = try {
            val response = httpClient.head(url)
            val contentType = response.headers["Content-Type"]
            val isImage = contentType?.startsWith("image/") == true
            when {
                response.status.value == 404 -> PhotoCheckStatus.Stale
                !response.status.isSuccess() -> PhotoCheckStatus.Error
                !isImage -> PhotoCheckStatus.Stale
                else -> PhotoCheckStatus.Ok
            }
        } catch (e: Exception) {
            PhotoCheckStatus.Error
        }
        updatePhotoStatus(id, status)
    }

    private fun updatePhotoStatus(id: String, status: PhotoCheckStatus) {
        _state.update { current ->
            current.copy(
                photos = current.photos.map { item ->
                    if (item.photo.id == id) item.copy(status = status) else item
                }
            )
        }
    }

    fun toggleSelection(id: String) {
        _state.update { state ->
            state.copy(
                photos = state.photos.map { item ->
                    if (item.photo.id == id) item.copy(selected = !item.selected) else item
                }
            )
        }
    }

    fun selectAllStale() {
        _state.update { state ->
            state.copy(
                photos = state.photos.map { item ->
                    if (item.status == PhotoCheckStatus.Stale || item.status == PhotoCheckStatus.Error) {
                        item.copy(selected = true)
                    } else item
                }
            )
        }
    }

    fun deselectAll() {
        _state.update { state ->
            state.copy(photos = state.photos.map { it.copy(selected = false) })
        }
    }

    fun deleteSelected() {
        val toDelete = _state.value.selectedPhotos
        if (toDelete.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true) }
            toDelete.forEach { item ->
                try {
                    firebasePhotos.deletePhoto(item.photo.id)
                    _state.update { state ->
                        state.copy(
                            photos = state.photos.filter { it.photo.id != item.photo.id },
                            deletedCount = state.deletedCount + 1
                        )
                    }
                } catch (e: Exception) {
                    Logger.e("AdminPhotosViewModel", e) { "Failed to delete photo ${item.photo.id}" }
                }
            }
            _state.update { it.copy(isDeleting = false) }
        }
    }
}
