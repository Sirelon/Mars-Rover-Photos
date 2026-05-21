@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.sirelon.marsroverphotos.platform

import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.utils.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import platform.Foundation.NSURL
import platform.Photos.PHAccessLevelAddOnly
import platform.Photos.PHAssetCreationRequest
import platform.Photos.PHAssetResourceTypePhoto
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHAuthorizationStatusNotDetermined
import platform.Photos.PHPhotoLibrary
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIModalPresentationFormSheet
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite
import kotlin.coroutines.resume

/**
 * iOS implementation of ImageOperations.
 *
 * saveImage:
 *   1. Requests add-only Photos permission (NSPhotoLibraryAddUsageDescription in Info.plist).
 *   2. Downloads the image via Ktor.
 *   3. Writes bytes to a temp file via POSIX.
 *   4. Calls PHPhotoLibrary.performChanges with PHAssetCreationRequest to write the asset.
 *   5. Removes the temp file.
 *
 * shareImage:
 *   Presents a native UIActivityViewController from the topmost UIViewController.
 *   Configures the iPad UIPopoverPresentationController anchor so the sheet is positioned correctly.
 */
class IosImageOperations : ImageOperations {

    // -------------------------------------------------------------------------
    // Save
    // -------------------------------------------------------------------------

    override suspend fun saveImage(photo: MarsImage): ImageOperationResult {
        return try {
            val authorized = requestAddOnlyPhotosPermission()
            if (!authorized) {
                return ImageOperationResult.Error(
                    "Photos permission denied. Please allow access in Settings > Privacy > Photos."
                )
            }

            // Download image bytes using Ktor (Darwin engine is already in iosMain deps).
            val bytes = downloadImageBytes(photo.imageUrl)
                ?: return ImageOperationResult.Error("Failed to download image")

            // Write bytes to a temp file so PHPhotoLibrary can import from a file URL.
            val uuid = NSUUID.UUID().UUIDString
            val tempPath = "${NSTemporaryDirectory()}mars_photo_$uuid.jpg"
            if (!writeBytesToFile(bytes, tempPath)) {
                return ImageOperationResult.Error("Failed to write temp image file")
            }
            val tempUrl = NSURL.fileURLWithPath(tempPath)

            // Save temp file to the Photos library.
            val result = suspendCancellableCoroutine<ImageOperationResult> { cont ->
                PHPhotoLibrary.sharedPhotoLibrary().performChanges(
                    changeBlock = {
                        val request = PHAssetCreationRequest.creationRequestForAsset()
                        request.addResourceWithType(
                            type = PHAssetResourceTypePhoto,
                            fileURL = tempUrl,
                            options = null
                        )
                    },
                    completionHandler = { success, error ->
                        if (success) {
                            Logger.d("IosImageOperations") { "Image saved to Photos" }
                            cont.resume(ImageOperationResult.Success("Photo saved to Photos library"))
                        } else {
                            val msg = error?.localizedDescription ?: "Unknown error saving photo"
                            Logger.e("IosImageOperations", null) { "Save failed: $msg" }
                            cont.resume(ImageOperationResult.Error(msg))
                        }
                    }
                )
            }

            // Remove the temp file regardless of success/failure.
            NSFileManager.defaultManager().removeItemAtPath(tempPath, error = null)
            result
        } catch (e: Exception) {
            Logger.e("IosImageOperations", e) { "Exception in saveImage" }
            ImageOperationResult.Error(e.message ?: "Unknown error while saving image")
        }
    }

    // -------------------------------------------------------------------------
    // Share
    // -------------------------------------------------------------------------

    override suspend fun shareImage(photo: MarsImage): ImageOperationResult {
        return try {
            val appUrl = "https://apps.apple.com/app/mars-rover-photos"
            val shareText = buildString {
                append("Take a look what I found on Mars!\n")
                append(photo.imageUrl)
                append("\n\nvia Mars Rover Photos app:\n")
                append(appUrl)
            }

            // Include both text and the direct image URL so share targets (Mail, Messages, etc.)
            // can display/preview the image inline.
            val imageNsUrl = NSURL.URLWithString(photo.imageUrl)
            val activityItems: List<*> =
                if (imageNsUrl != null) listOf(shareText, imageNsUrl) else listOf(shareText)

            val activityController = UIActivityViewController(
                activityItems = activityItems,
                applicationActivities = null
            )

            val rootVC = findTopViewController()
                ?: return ImageOperationResult.Error(
                    "Could not find a view controller to present the share sheet"
                )

            // On iPad, UIActivityViewController defaults to a popover that needs a source anchor.
            // UIViewController.popoverPresentationController lives in an ObjC category that
            // Kotlin/Native does not currently bridge directly.  Override the presentation
            // style to UIModalPresentationFormSheet so UIKit never tries to show a popover,
            // which avoids an NSInternalInconsistencyException on all device sizes.
            // Follow-up: once a Kotlin/Native bridge is available, anchor to the share button.
            activityController.modalPresentationStyle = UIModalPresentationFormSheet

            rootVC.presentViewController(activityController, animated = true, completion = null)
            Logger.d("IosImageOperations") { "Share sheet presented for ${photo.imageUrl}" }
            ImageOperationResult.Success()
        } catch (e: Exception) {
            Logger.e("IosImageOperations", e) { "Exception in shareImage" }
            ImageOperationResult.Error(e.message ?: "Unknown error while sharing image")
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Request add-only Photos permission (PHAccessLevelAddOnly, iOS 14+) and
     * return true if the user grants access.
     */
    private suspend fun requestAddOnlyPhotosPermission(): Boolean {
        val status = PHPhotoLibrary.authorizationStatusForAccessLevel(PHAccessLevelAddOnly)
        return when (status) {
            PHAuthorizationStatusAuthorized,
            PHAuthorizationStatusLimited -> true

            PHAuthorizationStatusNotDetermined -> {
                suspendCancellableCoroutine { cont ->
                    PHPhotoLibrary.requestAuthorizationForAccessLevel(
                        accessLevel = PHAccessLevelAddOnly,
                        handler = { newStatus ->
                            cont.resume(
                                newStatus == PHAuthorizationStatusAuthorized ||
                                    newStatus == PHAuthorizationStatusLimited
                            )
                        }
                    )
                }
            }

            else -> false // Denied or Restricted — user must change in Settings
        }
    }

    /**
     * Download raw image bytes using the Ktor Darwin HTTP client.
     * Returns null on network or HTTP error.
     */
    private suspend fun downloadImageBytes(urlString: String): ByteArray? {
        val client = HttpClient(Darwin)
        return try {
            client.get(urlString).readRawBytes().takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            Logger.e("IosImageOperations", e) { "Download failed: $urlString" }
            null
        } finally {
            client.close()
        }
    }

    /**
     * Write [bytes] to the file at [path] using POSIX fopen/fwrite/fclose.
     * Returns true on success, false on failure (file could not be opened or written).
     */
    private fun writeBytesToFile(bytes: ByteArray, path: String): Boolean {
        val file = fopen(path, "wb") ?: run {
            Logger.e("IosImageOperations", null) { "fopen failed for $path" }
            return false
        }
        return try {
            val written = bytes.usePinned { pinned ->
                fwrite(pinned.addressOf(0), 1uL, bytes.size.toULong(), file)
            }
            if (written != bytes.size.toULong()) {
                Logger.e("IosImageOperations", null) {
                    "Partial write for $path: expected=${bytes.size}, written=$written"
                }
                false
            } else {
                true
            }
        } catch (e: Exception) {
            Logger.e("IosImageOperations", e) { "fwrite failed for $path" }
            false
        } finally {
            fclose(file)
        }
    }

    /**
     * Walk the UIApplication scene / window hierarchy to find the topmost presented
     * UIViewController. This is the correct presenter for modal sheets and popovers.
     */
    private fun findTopViewController(): UIViewController? {
        // Prefer connected scenes (iOS 13+; also supports Stage Manager / multi-window).
        var rootVC: UIViewController? = null

        for (scene in UIApplication.sharedApplication.connectedScenes) {
            val windowScene = scene as? UIWindowScene ?: continue
            for (window in windowScene.windows) {
                val w = window as? UIWindow ?: continue
                if (w.isKeyWindow()) {
                    rootVC = w.rootViewController
                    break
                }
            }
            if (rootVC != null) break
        }

        // Fallback path for hosts that don't use UIScene (deprecated in iOS 15 but still works).
        if (rootVC == null) {
            @Suppress("DEPRECATION")
            rootVC = UIApplication.sharedApplication.windows
                .filterIsInstance<UIWindow>()
                .firstOrNull { it.isKeyWindow() }
                ?.rootViewController
        }

        // Walk to the topmost presented controller so we never present under an existing modal.
        var topVC = rootVC
        while (topVC?.presentedViewController != null) {
            topVC = topVC.presentedViewController
        }
        return topVC
    }
}

/**
 * Create an iOS Photos-library-backed ImageOperations instance.
 */
actual fun createImageOperations(): ImageOperations = IosImageOperations()
