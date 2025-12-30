package com.sirelon.marsroverphotos.platform

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.asDrawable
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.utils.Logger
import java.io.File

/**
 * Android implementation of ImageOperations.
 * Uses MediaStore for saving images and Intent.ACTION_SEND for sharing.
 */
class AndroidImageOperations(private val context: Context) : ImageOperations {

    @OptIn(ExperimentalCoilApi::class)
    override suspend fun saveImage(photo: MarsImage): ImageOperationResult {
        return try {
            // Load image using Coil
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(photo.imageUrl)
                .allowHardware(false) // Disable hardware bitmaps for saving
                .build()

            val result = loader.execute(request)
            if (result !is SuccessResult) {
                return ImageOperationResult.Error("Failed to load image")
            }

            val drawable = result.image.asDrawable(context.resources)
            val bitmap = (drawable as? BitmapDrawable)?.bitmap
                ?: return ImageOperationResult.Error("Failed to convert image to bitmap")

            // Save to MediaStore
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "MarsPhoto_${photo.id}.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}${File.separator}MarsRoverPhotos"
                    )
                }
            }

            val imageUri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: return ImageOperationResult.Error("Failed to create MediaStore entry")

            context.contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            } ?: return ImageOperationResult.Error("Failed to open output stream")

            Logger.d("AndroidImageOperations") { "Image saved successfully: $imageUri" }
            ImageOperationResult.Success(imageUri.toString())
        } catch (e: Exception) {
            Logger.e("AndroidImageOperations", e) { "Error saving image" }
            ImageOperationResult.Error(e.message ?: "Unknown error while saving image")
        }
    }

    override suspend fun shareImage(photo: MarsImage): ImageOperationResult {
        return try {
            val appUrl = "https://play.google.com/store/apps/details?id=${context.packageName}"
            val shareText = """
                Take a look what I found on Mars!
                ${photo.imageUrl}

                via Mars Rover Photos app:
                $appUrl
            """.trimIndent()

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Mars Photo").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(chooser)
            Logger.d("AndroidImageOperations") { "Share dialog opened" }
            ImageOperationResult.Success()
        } catch (e: Exception) {
            Logger.e("AndroidImageOperations", e) { "Error sharing image" }
            ImageOperationResult.Error(e.message ?: "Unknown error while sharing image")
        }
    }
}

private var androidContext: Context? = null

/**
 * Initialize Android image operations with application context.
 * Should be called once on app startup.
 */
fun initAndroidImageOperations(context: Context) {
    androidContext = context.applicationContext
}

/**
 * Create Android MediaStore-based image operations instance.
 */
actual fun createImageOperations(): ImageOperations {
    val context = androidContext
        ?: throw IllegalStateException("Android context not initialized. Call initAndroidImageOperations() first.")
    return AndroidImageOperations(context)
}
