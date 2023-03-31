@file:OptIn(ExperimentalCoroutinesApi::class)

package com.sirelon.marsroverphotos.feature.images

import android.app.Application
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.RoverApplication
import com.sirelon.marsroverphotos.extensions.exceptionHandler
import com.sirelon.marsroverphotos.extensions.recordException
import com.sirelon.marsroverphotos.feature.MultitouchDetectorCallback
import com.sirelon.marsroverphotos.storage.MarsImage
import com.sirelon.marsroverphotos.tracker.FullscreenImageTracker
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File


private val fullscreenImageTracker = FullscreenImageTracker()

/**
 * Created on 22.08.2020 18:59 for Mars-Rover-Photos.
 */
class ImageViewModel(app: Application) : AndroidViewModel(app),
    MultitouchDetectorCallback by fullscreenImageTracker {

    private val IO = Dispatchers.IO + exceptionHandler

    private val repository = ImagesRepository(app)

    private val idsEmitor = MutableStateFlow<List<String>>(emptyList())

    private val uiEventEmitter = Channel<UiEvent>(capacity = Channel.BUFFERED)
    val uiEvent: Flow<UiEvent> = uiEventEmitter.receiveAsFlow()

    var shouldTrack = true

    private val hideUiEmitter = MutableStateFlow(false)

    private val imagesFlow: Flow<ImmutableList<MarsImage>> = idsEmitor
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
        .mapLatest { it.toImmutableList() }
        .flowOn(IO)

    val screenState = combine(imagesFlow, hideUiEmitter, ::ImageScreenState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ImageScreenState()
        )

    fun setIdsToShow(ids: List<String>) {
        idsEmitor.value = ids
    }

    fun updateFavorite(image: MarsImage) {
        viewModelScope.launch(IO) {
            repository.updateFavForImage(image)
        }

        if (shouldTrack) {
            val tracker = getApplication<RoverApplication>().tracker
            tracker.trackFavorite(image, "Images", !image.favorite)
        }
    }

    @Suppress("DEPRECATION")
    fun saveImage(photo: MarsImage) {
        val activity = getApplication<RoverApplication>()
        viewModelScope.launch(IO) {
            kotlin.runCatching {
                val loader = ImageLoader(activity)
                val request = ImageRequest.Builder(activity)
                    .data(photo.imageUrl)
                    .allowHardware(false) // Disable hardware bitmaps.
                    .build()

                val result = (loader.execute(request) as SuccessResult).drawable
                val bitmap = (result as BitmapDrawable).bitmap

                val localUrl: String?
                val contentValues = ContentValues()
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "Image_" + ".jpg")
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        (Environment.DIRECTORY_PICTURES + File.separator) + "MarsRoverPhotos"
                    )
                }
                val imageUri = activity.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                activity.contentResolver.openOutputStream(imageUri!!).use {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                }
                localUrl = imageUri.toString()

                launch(exceptionHandler) {
                    val dataManager = RoverApplication.APP.dataManger
                    dataManager.updatePhotoSaveCounter(photo)
                }
                // Update counter for save
                localUrl
            }
                .onSuccess { uiEventEmitter.send(UiEvent.PhotoSaved(it)) }
                .onFailure {
                    recordException(it)
                    withContext(Dispatchers.Main) {
                        it.printStackTrace()
                        Toast.makeText(activity, "Error occurred ${it.message}", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
        }
    }

    fun onShown(marsPhoto: MarsImage, page: Int) {
        Timber.d("onShown() called with: marsPhoto = $marsPhoto, page = $page")
        val dataManger = RoverApplication.APP.dataManger
        dataManger.trackEvent("photo_show", mapOf("page" to page))

        viewModelScope.launch(IO) {
            dataManger.updatePhotoSeenCounter(marsPhoto)
        }
    }

    fun trackSaveClick() {
        if (shouldTrack) {
            getApplication<RoverApplication>().tracker.trackClick("save")
        }
    }

    override fun onTap() {
        hideUiEmitter.value = !hideUiEmitter.value
        fullscreenImageTracker.onTap()
    }

    override fun onZoomGesture(
        zoomToChange: Float,
        offsetY: Float,
        offsetX: Float,
        shouldBlock: Boolean
    ) {
        fullscreenImageTracker.onZoomGesture(zoomToChange, offsetY, offsetX, shouldBlock)
        if (zoomToChange != 1f) {
            hideUiEmitter.value = true
        }
    }

    fun shareMarsImage(marsImage: MarsImage) {
        val application = getApplication<RoverApplication>()
        if (shouldTrack) {
            application.tracker.trackClick("share")
        }

        viewModelScope.launch(IO) {
            RoverApplication.APP.dataManger.updatePhotoShareCounter(marsImage, null)
        }

        val shareIntent = shareIntent(marsImage)
        application.startActivity(
            Intent.createChooser(shareIntent, application.resources.getText(R.string.share))
                .apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
        )
    }

    private val appUrl by lazy {
        "https://play.google.com/store/apps/details?id=${getApplication<Application>().packageName}"
    }

    private fun shareIntent(marsImage: MarsImage): Intent {
        val shareIntent = Intent(Intent.ACTION_SEND)
        val shareText =
            "Take a look what I found on Mars ${marsImage.imageUrl} with this app \n\n$appUrl"
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText)
        return shareIntent
    }

    /**
     * Debug methods
     */
    internal fun makePopular(marsImage: MarsImage) {
        viewModelScope.launch(IO) {
            repository.makePopular(marsImage)
        }
    }

    internal fun removePopular(marsImage: MarsImage) {
        viewModelScope.launch(IO) {
            repository.removePopular(marsImage)
        }
    }
}

sealed class UiEvent {
    class PhotoSaved(val imagePath: String?) : UiEvent()
}


data class ImageScreenState(
    val images: ImmutableList<MarsImage> = persistentListOf(),
    val hideUi: Boolean = false,
)