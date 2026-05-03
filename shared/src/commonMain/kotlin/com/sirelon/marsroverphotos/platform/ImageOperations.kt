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
