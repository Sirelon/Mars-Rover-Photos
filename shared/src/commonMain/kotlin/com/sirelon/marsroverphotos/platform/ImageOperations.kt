package com.sirelon.marsroverphotos.platform

import com.sirelon.marsroverphotos.data.database.entities.MarsImage

/**
 * Platform-agnostic interface for image operations.
 * Provides functionality for saving and sharing images across all platforms.
 *
 * Implementations:
 * - Android: MediaStore + Share Intent
 * - iOS: Photo Library + UIActivityViewController
 * - Desktop: File dialogs
 * - Web: Download API + Web Share API
 */
interface ImageOperations {
    /**
     * Save an image to device storage.
     * Downloads the image from the URL and saves it to platform-specific location.
     *
     * @param photo The Mars image to save
     * @return Result with saved file path on success, or error message on failure
     */
    suspend fun saveImage(photo: MarsImage): ImageOperationResult

    /**
     * Share an image via platform share mechanisms.
     * On mobile: Opens native share sheet
     * On desktop: Copies share text to clipboard
     * On web: Uses Web Share API or copies to clipboard
     *
     * @param photo The Mars image to share
     * @return Result indicating success or failure
     */
    suspend fun shareImage(photo: MarsImage): ImageOperationResult

    /** Whether [setAsWallpaper] does anything here; the viewer hides the action when false. */
    val canSetWallpaper: Boolean

    /**
     * Offer the photo as the device wallpaper.
     *
     * Android opens the system wallpaper preview, where the user picks home screen, lock screen
     * or both. iOS has no public API for an app to set the wallpaper, so it saves the photo to
     * Photos and the viewer explains the remaining steps. Desktop does not support it.
     */
    suspend fun setAsWallpaper(photo: MarsImage): WallpaperResult
}

/** Result of [ImageOperations.setAsWallpaper]. */
sealed interface WallpaperResult {
    /** The system wallpaper screen opened; it reports nothing back. */
    data object Opened : WallpaperResult

    /** The photo is in Photos; the user sets it as wallpaper from there. */
    data object SavedToPhotos : WallpaperResult

    data class Error(val error: String) : WallpaperResult
}

/**
 * Result of an image operation.
 */
sealed class ImageOperationResult {
    /**
     * Operation completed successfully.
     * @param message Optional success message (e.g., file path)
     */
    data class Success(val message: String? = null) : ImageOperationResult()

    /**
     * Operation failed.
     * @param error Error message describing what went wrong
     */
    data class Error(val error: String) : ImageOperationResult()
}

/**
 * Factory function to create platform-specific image operations instance.
 */
expect fun createImageOperations(): ImageOperations
