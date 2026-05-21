package com.sirelon.marsroverphotos.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.toBitmap
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.data.network.RestApi
import com.sirelon.marsroverphotos.domain.models.CURIOSITY_ID
import com.sirelon.marsroverphotos.domain.models.INSIGHT_ID
import com.sirelon.marsroverphotos.domain.models.OPPORTUNITY_ID
import com.sirelon.marsroverphotos.domain.models.PERSEVERANCE_ID
import com.sirelon.marsroverphotos.domain.models.SPIRIT_ID
import com.sirelon.marsroverphotos.domain.models.PhotosQueryRequest
import com.sirelon.marsroverphotos.domain.repositories.ImagesRepository
import com.sirelon.marsroverphotos.platform.Tracker
import com.sirelon.marsroverphotos.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

public class MarsPhotoWidgetWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val imagesRepository: ImagesRepository by inject()
    private val api: RestApi by inject()
    @Suppress("UnusedPrivateProperty")
    private val tracker: Tracker by inject()

    override suspend fun doWork(): Result {
        val glanceManager = GlanceAppWidgetManager(applicationContext)
        val glanceIds = glanceManager.getGlanceIds(MarsPhotoWidget::class.java)
        if (glanceIds.isEmpty()) {
            return Result.success()
        }

        val widgetDir = File(applicationContext.cacheDir, "mars_widget")

        var updatedCount = 0
        var hadFailure = false

        glanceIds.forEach { glanceId ->
            val state = getAppWidgetState(applicationContext, PreferencesGlanceStateDefinition, glanceId)
            val roverId = state[MarsPhotoWidgetState.roverIdKey] ?: DefaultRoverId

            val latestPhoto = try {
                fetchLatestPhoto(roverId)
            } catch (error: Exception) {
                hadFailure = true
                Logger.e("MarsPhotoWidgetWorker", error) { "Failed to load latest photo for rover $roverId" }
                null
            }

            if (latestPhoto == null) {
                return@forEach
            }

            val bitmap = loadBitmap(applicationContext, latestPhoto.imageUrl)
            val imagePath = bitmap?.let { saveBitmap(widgetDir, glanceId.hashCode().toString(), it) }

            try {
                imagesRepository.saveImages(listOf(latestPhoto))
                if (imagePath == null) {
                    hadFailure = true
                    return@forEach
                }
                updateAppWidgetState(applicationContext, glanceId) { prefs ->
                    prefs[MarsPhotoWidgetState.roverIdKey] = roverId
                    prefs[MarsPhotoWidgetState.roverNameKey] = roverNameForId(roverId)
                    prefs[MarsPhotoWidgetState.imageIdKey] = latestPhoto.id
                    prefs[MarsPhotoWidgetState.solKey] = latestPhoto.sol
                    prefs[MarsPhotoWidgetState.earthDateKey] = latestPhoto.earthDate
                    prefs[MarsPhotoWidgetState.imagePathKey] = imagePath
                }
                MarsPhotoWidget().update(applicationContext, glanceId)
                updatedCount += 1
            } catch (error: Exception) {
                hadFailure = true
                Logger.e("MarsPhotoWidgetWorker", error) { "Failed to update widget state for rover $roverId" }
            }
        }

        return if (updatedCount == 0 && hadFailure) {
            Result.retry()
        } else {
            Result.success()
        }
    }

    private suspend fun fetchLatestPhoto(roverId: Long): MarsImage? {
        return when (roverId) {
            INSIGHT_ID -> api.getInsightLatestPhotos().firstOrNull()
            PERSEVERANCE_ID -> api.getPerseveranceLatestPhotos().firstOrNull()
            CURIOSITY_ID, OPPORTUNITY_ID, SPIRIT_ID -> fetchLatestByManifest(roverId)
            else -> fetchLatestByManifest(roverId)
        }
    }

    private suspend fun fetchLatestByManifest(roverId: Long): MarsImage? {
        val roverName = when (roverId) {
            CURIOSITY_ID -> "curiosity"
            OPPORTUNITY_ID -> "opportunity"
            SPIRIT_ID -> "spirit"
            PERSEVERANCE_ID -> "perseverance"
            else -> "curiosity"
        }
        val roverInfo = api.getRoverInfo(roverName)
        var sol = roverInfo.maxSol

        repeat(MaxSolLookback) {
            val photos = api.getRoversPhotos(PhotosQueryRequest(roverId, sol, null))
            if (photos.isNotEmpty()) {
                return photos.first()
            }
            sol -= 1
            if (sol <= 0) {
                return null
            }
        }
        return null
    }

    private suspend fun loadBitmap(context: Context, url: String): Bitmap? = withContext(Dispatchers.IO) {
        if (url.isBlank()) {
            return@withContext null
        }
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(url)
            .size(640)
            .build()
        val result = loader.execute(request)
        result.image?.toBitmap()
    }

    private fun saveBitmap(directory: File, key: String, bitmap: Bitmap): String? {
        if (!directory.exists() && !directory.mkdirs()) {
            return null
        }
        val file = File(directory, "mars_photo_widget_$key.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
        }
        return file.absolutePath
    }

    companion object {
        private const val UniqueWorkName = "mars_photo_widget_daily"
        private const val UniqueImmediateWork = "mars_photo_widget_now"
        private const val MaxSolLookback = 30

        fun enqueuePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<MarsPhotoWidgetWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UniqueWorkName,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun enqueueOnce(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<MarsPhotoWidgetWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UniqueImmediateWork,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
