package com.sirelon.marsroverphotos.data.repositories

import com.sirelon.marsroverphotos.domain.models.ROVER_DISPLAY_ORDER
import com.sirelon.marsroverphotos.domain.repositories.PhotosRepository
import com.sirelon.marsroverphotos.domain.repositories.RoverCoversRepository
import com.sirelon.marsroverphotos.domain.repositories.RoverCoversRepository.Cover
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
 * Keeps the rover cards' photos: each rover's latest photo (URL, sol and date), saved in
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

    private val _covers = MutableStateFlow(
        roverIds.mapNotNull { id -> savedCover(id)?.let { id to it } }.toMap()
    )
    override val covers: StateFlow<Map<Long, Cover>> = _covers.asStateFlow()

    // Job.start() is idempotent: the first refreshOnce() starts it, every later call is a no-op.
    private val refreshJob = appScope.launch(start = CoroutineStart.LAZY) {
        roverIds.forEach { id -> launch { refresh(id) } }
    }

    override fun refreshOnce() {
        refreshJob.start()
    }

    private suspend fun refresh(roverId: Long) {
        val photo = try {
            photosRepository.latestPhoto(roverId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w(TAG) { "No latest photo for rover $roverId: ${e.message}" }
            null
        }
        if (photo == null || photo.imageUrl.isBlank()) return
        val cover = Cover(imageUrl = photo.imageUrl, sol = photo.sol, earthDate = photo.earthDate)
        if (cover == _covers.value[roverId]) return
        preferences.setString(urlKey(roverId), cover.imageUrl)
        preferences.setLong(solKey(roverId), cover.sol)
        preferences.setString(dateKey(roverId), cover.earthDate)
        _covers.update { it + (roverId to cover) }
    }

    private fun savedCover(roverId: Long): Cover? {
        val url = preferences.getString(urlKey(roverId), "").takeIf { it.isNotBlank() } ?: return null
        return Cover(
            imageUrl = url,
            sol = preferences.getLong(solKey(roverId), 0L),
            earthDate = preferences.getString(dateKey(roverId), ""),
        )
    }

    private fun urlKey(roverId: Long) = "roverCoverUrl_$roverId"
    private fun solKey(roverId: Long) = "roverCoverSol_$roverId"
    private fun dateKey(roverId: Long) = "roverCoverDate_$roverId"

    private companion object {
        const val TAG = "RoverCoversRepository"
    }
}
