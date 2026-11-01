package com.sirelon.marsroverphotos.data.repositories

import com.sirelon.marsroverphotos.domain.models.ROVER_DISPLAY_ORDER
import com.sirelon.marsroverphotos.domain.repositories.PhotosRepository
import com.sirelon.marsroverphotos.domain.repositories.RoverCoversRepository
import com.sirelon.marsroverphotos.platform.PlatformPreferences
import com.sirelon.marsroverphotos.utils.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Keeps the rover cards' photos: the URL of each rover's latest photo, saved in
 * [PlatformPreferences] so the next cold start can show it before any request. The image bytes
 * themselves are cached by Coil's disk cache under that URL.
 *
 * The refresh runs once per process, in [appScope], one coroutine per rover. A slow source (the
 * Viking images on planetarydata.jpl.nasa.gov) or a failing one therefore never holds back the
 * other cards. Bound as a Koin `single`, so every RoversViewModel shares the one refresh.
 */
class RoverCoversRepositoryImpl(
    private val photosRepository: PhotosRepository,
    private val preferences: PlatformPreferences,
    appScope: CoroutineScope,
    private val roverIds: List<Long> = ROVER_DISPLAY_ORDER,
) : RoverCoversRepository {

    private val _coverUrls = MutableStateFlow(
        roverIds.mapNotNull { id ->
            preferences.getString(key(id), "").takeIf { it.isNotBlank() }?.let { id to it }
        }.toMap()
    )
    override val coverUrls: StateFlow<Map<Long, String>> = _coverUrls.asStateFlow()

    // Job.start() is idempotent: the first refreshOnce() starts it, every later call is a no-op.
    private val refreshJob = appScope.launch(start = CoroutineStart.LAZY) {
        roverIds.forEach { id -> launch { refresh(id) } }
    }

    override fun refreshOnce() {
        refreshJob.start()
    }

    private suspend fun refresh(roverId: Long) {
        val url = try {
            photosRepository.latestPhoto(roverId)?.imageUrl
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w(TAG) { "No latest photo for rover $roverId: ${e.message}" }
            null
        }
        if (url.isNullOrBlank() || url == _coverUrls.value[roverId]) return
        preferences.setString(key(roverId), url)
        _coverUrls.update { it + (roverId to url) }
    }

    private fun key(roverId: Long) = "roverCoverUrl_$roverId"

    private companion object {
        const val TAG = "RoverCoversRepository"
    }
}
