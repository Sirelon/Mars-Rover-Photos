import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.ComposeUIViewController
import com.sirelon.marsroverphotos.platform.BuildInfo
import com.sirelon.marsroverphotos.presentation.App
import com.sirelon.marsroverphotos.presentation.navigation.DeepLink
import com.sirelon.marsroverphotos.presentation.navigation.parseDeepLink
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
    pendingDeepLink.value = parseDeepLink(urlString) ?: return
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
            appVersion = BuildInfo.versionName,
            rateAppUrl = "https://apps.apple.com/app/mars-rover-photos"
        )
    }
}
