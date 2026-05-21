package com.sirelon.marsroverphotos

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.sirelon.marsroverphotos.presentation.App
import com.sirelon.marsroverphotos.presentation.navigation.DeepLink
import com.sirelon.marsroverphotos.utils.Logger
import com.sirelon.marsroverphotos.widget.WidgetExtraImageId

/**
 * Main activity for the Mars Rover Photos app.
 * Hosts the Compose UI from the shared module.
 */
class MainActivity : ComponentActivity() {
    private var pendingDeepLink: DeepLink? by mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install Android 12+ splash screen
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Prevent the system from adding a translucent scrim over the NavigationBar —
        // the app's NavigationBar background provides the necessary contrast.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        // Handle deep link if present
        handleDeepLink(intent)

        setContent {
            App(
                deepLink = pendingDeepLink,
                onDeepLinkConsumed = { pendingDeepLink = null },
                onRateApp = ::openStoreListing,
                appVersion = BuildConfig.VERSION_NAME
            )
        }
    }

    private fun openStoreListing() {
        val packageName = applicationContext.packageName
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (_: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
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
        // Check for widget photo extra first
        val widgetImageId = intent?.getStringExtra(WidgetExtraImageId)
        if (!widgetImageId.isNullOrBlank()) {
            Logger.d("MainActivity") { "Widget deep link received: $widgetImageId" }
            pendingDeepLink = DeepLink.Image(widgetImageId)
            return
        }

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
                    val id = roverId.toLong()
                    Logger.d("MainActivity") { "Navigate to rover: $roverId" }
                    pendingDeepLink = DeepLink.Rover(id)
                } else {
                    Logger.w("MainActivity") { "Invalid rover ID in deep link: $roverId" }
                }
            }
            "photo" -> {
                // marsrover://photo/{photoId}
                val photoId = pathSegments.firstOrNull()
                if (photoId != null && photoId.toLongOrNull() != null) {
                    val id = photoId.toLong()
                    Logger.d("MainActivity") { "Navigate to photo: $photoId" }
                    pendingDeepLink = DeepLink.Photo(id)
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
                                val roverId = id.toLong()
                                Logger.d("MainActivity") { "Navigate to rover (web): $id" }
                                pendingDeepLink = DeepLink.Rover(roverId)
                            } else {
                                Logger.w("MainActivity") { "Invalid rover ID in web deep link: $id" }
                            }
                        }
                        "photo" -> {
                            if (id.toLongOrNull() != null) {
                                val photoId = id.toLong()
                                Logger.d("MainActivity") { "Navigate to photo (web): $id" }
                                pendingDeepLink = DeepLink.Photo(photoId)
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
