import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.ComposeUIViewController
import com.sirelon.marsroverphotos.presentation.App
import com.sirelon.marsroverphotos.presentation.navigation.DeepLink
import kotlinx.coroutines.flow.MutableStateFlow
import platform.Foundation.NSURL
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
    val url = NSURL.URLWithString(urlString) ?: return
    val host = url.host ?: return
    val pathSegments = url.pathComponents
        ?.mapNotNull { it as? String }
        ?.filter { it != "/" && it.isNotBlank() }
        .orEmpty()
    if (pathSegments.isEmpty()) return

    val (kind, idStr) = when (host.lowercase()) {
        "marsroverphotos.app" -> {
            if (pathSegments.size < 2) return
            pathSegments[0] to pathSegments[1]
        }

        "rover", "photo" -> host.lowercase() to pathSegments[0]
        else -> return
    }
    val deepLink = when (kind) {
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
