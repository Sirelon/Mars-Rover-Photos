package com.sirelon.marsroverphotos.platform

import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.asDrawable
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Android implementation of ImageOperations.
 * Uses MediaStore for saving images and Intent.ACTION_SEND for sharing.
 */
class AndroidImageOperations(private val context: Context) : ImageOperations {

    override suspend fun saveImage(photo: MarsImage): ImageOperationResult {
        return try {
            val bitmap = loadBitmap(photo.imageUrl)
                ?: return ImageOperationResult.Error("Failed to load image")

            // MediaStore insert and the full-quality JPEG encode are blocking I/O; off the caller's
            // (Main) dispatcher, they were an ANR source.
            withContext(Dispatchers.IO) {
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
                ) ?: return@withContext ImageOperationResult.Error("Failed to create MediaStore entry")

                context.contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                } ?: return@withContext ImageOperationResult.Error("Failed to open output stream")

                Logger.d("AndroidImageOperations") { "Image saved successfully: $imageUri" }
                ImageOperationResult.Success(imageUri.toString())
            }
        } catch (e: Exception) {
            Logger.e("AndroidImageOperations", e) { "Error saving image" }
            ImageOperationResult.Error(e.message ?: "Unknown error while saving image")
        }
    }

    override val canSetWallpaper: Boolean = true

    override suspend fun setAsWallpaper(photo: MarsImage): WallpaperResult {
        return try {
            val bitmap = loadBitmap(photo.imageUrl) ?: return WallpaperResult.Error("Failed to load image")
            val uri = withContext(Dispatchers.IO) {
                val dir = File(context.cacheDir, WALLPAPER_DIR).apply { mkdirs() }
                val file = File(dir, WALLPAPER_FILE)
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, WALLPAPER_JPEG_QUALITY, it) }
                FileProvider.getUriForFile(context, "${context.packageName}$WALLPAPER_AUTHORITY_SUFFIX", file)
            }
            openWallpaperScreen(uri)
            WallpaperResult.Opened
        } catch (e: Exception) {
            Logger.e("AndroidImageOperations", e) { "Error setting wallpaper" }
            WallpaperResult.Error(e.message ?: "Unknown error while setting wallpaper")
        }
    }

    /**
     * Opens the system wallpaper preview, where the user picks home screen, lock screen or both.
     * Some OEM builds have no handler for it; they get the system "Set as" picker instead.
     */
    private fun openWallpaperScreen(uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        val cropAndSet = runCatching {
            WallpaperManager.getInstance(context).getCropAndSetWallpaperIntent(uri)
        }.getOrNull()
        if (cropAndSet != null) {
            try {
                context.startActivity(cropAndSet.addFlags(flags))
                return
            } catch (e: ActivityNotFoundException) {
                Logger.e("AndroidImageOperations", e) { "No wallpaper cropper; falling back to Set as" }
            }
        }
        val attach = Intent(Intent.ACTION_ATTACH_DATA)
            .setDataAndType(uri, JPEG_MIME)
            .putExtra("mimeType", JPEG_MIME)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(attach, "Set as wallpaper").addFlags(flags))
    }

    /** Downloads [url] through Coil into a software bitmap that can be compressed. */
    @OptIn(ExperimentalCoilApi::class)
    private suspend fun loadBitmap(url: String): Bitmap? {
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false) // hardware bitmaps cannot be compressed
            .build()
        val result = ImageLoader(context).execute(request) as? SuccessResult ?: return null
        return (result.image.asDrawable(context.resources) as? BitmapDrawable)?.bitmap
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

// Must match the FileProvider declared in androidApp's AndroidManifest.xml and its xml/wallpaper_paths.xml.
private const val WALLPAPER_AUTHORITY_SUFFIX = ".wallpaper"
private const val WALLPAPER_DIR = "wallpaper"
private const val WALLPAPER_FILE = "wallpaper.jpg"
private const val WALLPAPER_JPEG_QUALITY = 95
private const val JPEG_MIME = "image/jpeg"

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

