package com.sirelon.marsroverphotos.platform

import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.utils.Logger
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.url.URL

/**
 * Web implementation of ImageOperations.
 * Uses download links for saving and Web Share API for sharing.
 */
class WebImageOperations : ImageOperations {

    override suspend fun saveImage(photo: MarsImage): ImageOperationResult {
        return try {
            // Create a temporary anchor element to trigger download
            val link = document.createElement("a") as HTMLAnchorElement
            link.href = photo.imageUrl
            link.download = "MarsPhoto_${photo.id}.jpg"
            link.style.display = "none"

            document.body?.appendChild(link)
            link.click()
            document.body?.removeChild(link)

            Logger.d("WebImageOperations") { "Image download triggered: ${photo.imageUrl}" }
            ImageOperationResult.Success("Image download started")
        } catch (e: Exception) {
            Logger.e("WebImageOperations", e as? Throwable) { "Error saving image" }
            ImageOperationResult.Error(e.message ?: "Unknown error while downloading image")
        }
    }

    override suspend fun shareImage(photo: MarsImage): ImageOperationResult {
        return try {
            val shareText = """
                Take a look what I found on Mars!
                ${photo.imageUrl}

                via Mars Rover Photos web app
            """.trimIndent()

            // Check if Web Share API is available
            val navigator = window.navigator
            if (js("navigator.share") != undefined) {
                // Use Web Share API
                val shareData = js("""({
                    title: 'Mars Rover Photo',
                    text: shareText,
                    url: photo.imageUrl
                })""")

                js("navigator.share(shareData)")
                    .then {
                        Logger.d("WebImageOperations") { "Shared successfully via Web Share API" }
                    }
                    .catch { error: dynamic ->
                        Logger.e("WebImageOperations", null) { "Error sharing: ${error.message}" }
                    }

                ImageOperationResult.Success("Share dialog opened")
            } else {
                // Fallback: Copy to clipboard
                window.navigator.asDynamic().clipboard?.writeText(shareText)
                    ?.then {
                        Logger.d("WebImageOperations") { "Share text copied to clipboard" }
                        window.alert("Share text copied to clipboard!")
                    }
                    ?: run {
                        Logger.w("WebImageOperations") { "Clipboard API not available" }
                        window.alert("Web Share not supported. Share text:\n\n$shareText")
                    }

                ImageOperationResult.Success("Share text copied to clipboard")
            }
        } catch (e: Exception) {
            Logger.e("WebImageOperations", e as? Throwable) { "Error sharing image" }
            ImageOperationResult.Error(e.message ?: "Unknown error while sharing image")
        }
    }
}

/**
 * Create Web download API-based image operations instance.
 */
actual fun createImageOperations(): ImageOperations {
    return WebImageOperations()
}
