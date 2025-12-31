package com.sirelon.marsroverphotos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.sirelon.marsroverphotos.presentation.App
import com.sirelon.marsroverphotos.utils.Logger

/**
 * Main activity for the Mars Rover Photos app.
 * Hosts the Compose UI from the shared module.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install Android 12+ splash screen
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle deep link if present
        handleDeepLink(intent)

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    /**
     * Handle deep link URIs.
     * Supports:
     * - marsrover://rover/{roverId}
     * - marsrover://photo/{photoId}
     * - https://marsroverphotos.app/rover/{roverId}
     * - https://marsroverphotos.app/photo/{photoId}
     */
    private fun handleDeepLink(intent: Intent?) {
        val data: Uri? = intent?.data
        if (data == null) {
            Logger.d("MainActivity") { "No deep link data" }
            return
        }

        Logger.d("MainActivity") { "Deep link received: $data" }

        val host = data.host ?: return
        val pathSegments = data.pathSegments

        when (host) {
            "rover" -> {
                // marsrover://rover/{roverId}
                val roverId = pathSegments.firstOrNull()
                if (roverId != null && roverId.toLongOrNull() != null) {
                    Logger.d("MainActivity") { "Navigate to rover: $roverId" }
                    // TODO: Navigate to rover screen with ID: $roverId
                } else {
                    Logger.w("MainActivity") { "Invalid rover ID in deep link: $roverId" }
                }
            }
            "photo" -> {
                // marsrover://photo/{photoId}
                val photoId = pathSegments.firstOrNull()
                if (photoId != null && photoId.toLongOrNull() != null) {
                    Logger.d("MainActivity") { "Navigate to photo: $photoId" }
                    // TODO: Navigate to photo screen with ID: $photoId
                } else {
                    Logger.w("MainActivity") { "Invalid photo ID in deep link: $photoId" }
                }
            }
            "marsroverphotos.app" -> {
                // https://marsroverphotos.app/rover/{roverId} or /photo/{photoId}
                if (pathSegments.size >= 2) {
                    val type = pathSegments[0] // "rover" or "photo"
                    val id = pathSegments[1]

                    when (type) {
                        "rover" -> {
                            if (id.toLongOrNull() != null) {
                                Logger.d("MainActivity") { "Navigate to rover (web): $id" }
                                // TODO: Navigate to rover screen with ID: $id
                            } else {
                                Logger.w("MainActivity") { "Invalid rover ID in web deep link: $id" }
                            }
                        }
                        "photo" -> {
                            if (id.toLongOrNull() != null) {
                                Logger.d("MainActivity") { "Navigate to photo (web): $id" }
                                // TODO: Navigate to photo screen with ID: $id
                            } else {
                                Logger.w("MainActivity") { "Invalid photo ID in web deep link: $id" }
                            }
                        }
                        else -> {
                            Logger.w("MainActivity") { "Unknown deep link type: $type" }
                        }
                    }
                } else {
                    Logger.w("MainActivity") { "Invalid web deep link path: ${data.path}" }
                }
            }
            else -> {
                Logger.w("MainActivity") { "Unknown deep link host: $host" }
            }
        }
    }
}
