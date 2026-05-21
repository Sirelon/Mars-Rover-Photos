import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.ComposeUIViewController
import com.sirelon.marsroverphotos.presentation.App
import com.sirelon.marsroverphotos.presentation.navigation.DeepLink
import kotlinx.coroutines.flow.MutableStateFlow
import platform.UIKit.UIViewController

/**
 * Internal bus so the iOS app shell can push deep links into the running Compose content
 * without recreating the UIViewController.
 */
private val pendingDeepLink = MutableStateFlow<DeepLink?>(null)

/**
 * Called by the iOS app shell (MarsRoverApp.swift) when the OS delivers a deep-link URL.
 *
 * Supported schemes:
 *   marsrover://rover/{roverId}   — navigate to a rover's photo grid
 *   marsrover://photo/{photoId}   — navigate directly to a photo in the gallery
 */
fun pushDeepLink(urlString: String) {
    if (!urlString.startsWith("marsrover://")) return
    val path = urlString.removePrefix("marsrover://")
    val parts = path.split("/")
    if (parts.size < 2) return
    val host = parts[0]
    val idStr = parts[1]
    val deepLink = when (host) {
        "rover" -> idStr.toLongOrNull()?.let { DeepLink.Rover(it) }
        "photo" -> idStr.toLongOrNull()?.let { DeepLink.Photo(it) }
        else -> null
    } ?: return
    pendingDeepLink.value = deepLink
}

/**
 * Main entry point for iOS app.
 * Creates a UIViewController hosting the Compose UI.
 */
fun MainViewController(): UIViewController {
    return ComposeUIViewController {
        val deepLink by pendingDeepLink.collectAsState()
        App(
            deepLink = deepLink,
            onDeepLinkConsumed = { pendingDeepLink.value = null },
            rateAppUrl = "https://apps.apple.com/app/mars-rover-photos"
        )
    }
}
