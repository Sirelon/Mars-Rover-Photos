package com.sirelon.marsroverphotos.platform

import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.utils.Logger
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.net.URL
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Desktop implementation of ImageOperations.
 * Uses Swing file dialogs for saving and clipboard for sharing.
 */
class DesktopImageOperations : ImageOperations {

    override suspend fun saveImage(photo: MarsImage): ImageOperationResult {
        return try {
            // Download image from URL
            val url = URL(photo.imageUrl)
            val image = ImageIO.read(url)
                ?: return ImageOperationResult.Error("Failed to download image from URL")

            // Show save dialog
            val fileChooser = JFileChooser().apply {
                dialogTitle = "Save Mars Photo"
                fileFilter = FileNameExtensionFilter("JPEG Images", "jpg", "jpeg")
                selectedFile = File("MarsPhoto_${photo.id}.jpg")
            }

            val result = fileChooser.showSaveDialog(null)
            if (result != JFileChooser.APPROVE_OPTION) {
                return ImageOperationResult.Error("Save cancelled by user")
            }

            val file = fileChooser.selectedFile
            if (!file.name.lowercase().endsWith(".jpg") && !file.name.lowercase().endsWith(".jpeg")) {
                return ImageOperationResult.Error("File must have .jpg or .jpeg extension")
            }

            // Save image to selected file
            val saved = ImageIO.write(image, "jpg", file)
            if (!saved) {
                return ImageOperationResult.Error("Failed to save image to file")
            }

            Logger.d("DesktopImageOperations") { "Image saved successfully: ${file.absolutePath}" }
            ImageOperationResult.Success(file.absolutePath)
        } catch (e: Exception) {
            Logger.e("DesktopImageOperations", e) { "Error saving image" }
            ImageOperationResult.Error(e.message ?: "Unknown error while saving image")
        }
    }

    override suspend fun shareImage(photo: MarsImage): ImageOperationResult {
        return try {
            val shareText = """
                Take a look what I found on Mars!
                ${photo.imageUrl}

                via Mars Rover Photos desktop app
            """.trimIndent()

            // Copy share text to clipboard
            val selection = StringSelection(shareText)
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(selection, selection)

            // Show confirmation dialog
            JOptionPane.showMessageDialog(
                null,
                "Share text copied to clipboard!\nYou can now paste it anywhere.",
                "Share Mars Photo",
                JOptionPane.INFORMATION_MESSAGE
            )

            Logger.d("DesktopImageOperations") { "Share text copied to clipboard" }
            ImageOperationResult.Success("Share text copied to clipboard")
        } catch (e: Exception) {
            Logger.e("DesktopImageOperations", e) { "Error sharing image" }
            ImageOperationResult.Error(e.message ?: "Unknown error while sharing image")
        }
    }
}

/**
 * Create Desktop file dialog-based image operations instance.
 */
actual fun createImageOperations(): ImageOperations {
    return DesktopImageOperations()
}
