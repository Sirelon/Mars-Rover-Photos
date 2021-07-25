package com.sirelon.marsroverphotos.feature.images

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.sirelon.marsroverphotos.RoverApplication
import com.sirelon.marsroverphotos.extensions.recordException
import com.sirelon.marsroverphotos.feature.MultitouchDetectorCallback
import com.sirelon.marsroverphotos.storage.MarsImage
import com.sirelon.marsroverphotos.tracker.FullscreenImageTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created on 22.08.2020 18:59 for Mars-Rover-Photos.
 */
class ImageViewModel(app: Application) : AndroidViewModel(app),
    MultitouchDetectorCallback by FullscreenImageTracker() {

    private val repository = ImagesRepository(app)

    private val idsEmitor = MutableStateFlow<List<String>>(emptyList())

    val uiEvent = MutableLiveData<UiEvent?>()

    val imagesLiveData = idsEmitor
        .flatMapLatest { repository.loadImages(it) }
        .flatMapLatest {
            if (it.isEmpty()) {
                recordException(IllegalStateException("Room is empty"))
                delay(500)
                repository.loadImages(idsEmitor.value)
            } else {
                flowOf(it)
            }
        }
        .asLiveData()

    fun setIdsToShow(ids: List<String>) {
        idsEmitor.value = ids
    }

    fun updateFavorite(image: MarsImage) {
        viewModelScope.launch {
            kotlin.runCatching { repository.updateFavForImage(image) }
                .onFailure {
                    it.printStackTrace()
                }
        }

        val tracker = getApplication<RoverApplication>().tracker
        tracker.trackFavorite(image.toMarsPhoto(), "Images", !image.favorite)
    }

    fun saveImage(activity: FragmentActivity, photo: MarsImage) {
        val appUrl = "https://play.google.com/store/apps/details?id=${activity.packageName}"
        viewModelScope.launch(Dispatchers.IO) {
            kotlin.runCatching {
                val bitmap =
                    Glide.with(activity).asBitmap().load(photo.imageUrl).submit().get()
                val localUrl = MediaStore.Images.Media.insertImage(
                    activity.contentResolver, bitmap,
                    "mars_photo_${photo.id}",
                    "Photo saved from $appUrl"
                )
                activity.sendBroadcast(
                    Intent(
                        Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                        Uri.parse(localUrl)
                    )
                )
                val dataManager = RoverApplication.APP.dataManger
                dataManager.updatePhotoSaveCounter(photo)
                // Update counter for save
                localUrl
            }
                .onSuccess { uiEvent.postValue(UiEvent.PhotoSaved(it)) }
                .onFailure {
                    withContext(Dispatchers.Main) {
                        it.printStackTrace()
                        Toast.makeText(activity, "Error occured ${it.message}", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
        }
    }

    fun onPermissionDenied(rationale: Boolean) {
//        uiEvent.value = null
        if (!rationale)
            uiEvent.value = UiEvent.CameraPermissionDenied(rationale)
    }

    fun onShown(marsPhoto: MarsImage, page: Int) {
        RoverApplication.APP.dataManger.updatePhotoSeenCounter(marsPhoto.toMarsPhoto())
    }
}

sealed class UiEvent {
    var handled = false

    class PhotoSaved(val imagePath: String?) : UiEvent()
    class CameraPermissionDenied(val rationale: Boolean) : UiEvent()
}