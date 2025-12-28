package com.sirelon.marsroverphotos.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.PreferencesGlanceStateDefinition
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
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
import com.sirelon.marsroverphotos.feature.images.ImagesRepository
import com.sirelon.marsroverphotos.feature.rovers.CuriosityId
import com.sirelon.marsroverphotos.feature.rovers.InsightId
import com.sirelon.marsroverphotos.feature.rovers.OpportunityId
import com.sirelon.marsroverphotos.feature.rovers.PerserveranceId
import com.sirelon.marsroverphotos.feature.rovers.SpiritId
import com.sirelon.marsroverphotos.models.PhotosQueryRequest
import com.sirelon.marsroverphotos.network.RestApi
import com.sirelon.marsroverphotos.storage.MarsImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

public class MarsPhotoWidgetWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val glanceManager = GlanceAppWidgetManager(applicationContext)
        val glanceIds = glanceManager.getGlanceIds(MarsPhotoWidget::class.java)
        if (glanceIds.isEmpty()) {
            return Result.success()
        }

        val api = RestApi()
        val imagesRepository = ImagesRepository(applicationContext)
        val widgetDir = File(applicationContext.cacheDir, "mars_widget")

        var updatedCount = 0
        var hadFailure = false

        glanceIds.forEach { glanceId ->
            val state = getAppWidgetState(applicationContext, glanceId, PreferencesGlanceStateDefinition)
            val roverId = state[MarsPhotoWidgetState.roverIdKey] ?: DefaultRoverId

            val latestPhoto = try {
                fetchLatestPhoto(api, roverId)
            } catch (error: Exception) {
                hadFailure = true
                Timber.e(error, "Failed to load latest photo for rover $roverId")
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
                updateAppWidgetState(applicationContext, glanceId, PreferencesGlanceStateDefinition) { prefs ->
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
                Timber.e(error, "Failed to update widget state for rover $roverId")
            }
        }

        return if (updatedCount == 0 && hadFailure) {
            Result.retry()
        } else {
            Result.success()
        }
    }

    private suspend fun fetchLatestPhoto(api: RestApi, roverId: Long): MarsImage? {
        return when (roverId) {
            InsightId -> api.getInsightLatestPhotos().firstOrNull()
            PerserveranceId -> api.getPerseveranceLatestPhotos().firstOrNull()
            CuriosityId, OpportunityId, SpiritId -> fetchLatestByManifest(api, roverId)
            else -> fetchLatestByManifest(api, roverId)
        }
    }

    private suspend fun fetchLatestByManifest(api: RestApi, roverId: Long): MarsImage? {
        val roverName = when (roverId) {
            CuriosityId -> "curiosity"
            OpportunityId -> "opportunity"
            SpiritId -> "spirit"
            PerserveranceId -> "perseverance"
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
            .allowHardware(false)
            .size(640)
            .build()
        val result = loader.execute(request)
        (result.drawable as? BitmapDrawable)?.bitmap
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
