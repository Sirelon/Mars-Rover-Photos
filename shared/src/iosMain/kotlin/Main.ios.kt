import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.ComposeUIViewController
import com.sirelon.marsroverphotos.domain.settings.AppSettings
import com.sirelon.marsroverphotos.platform.BuildInfo
import com.sirelon.marsroverphotos.platform.PushNotifications
import com.sirelon.marsroverphotos.presentation.App
import com.sirelon.marsroverphotos.presentation.navigation.DeepLink
import com.sirelon.marsroverphotos.presentation.navigation.parseDeepLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform
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
 * Called by the app delegate once FCM holds a registration token — the earliest point a topic
 * subscription can succeed, since FCM refuses to mint a token before APNs has issued a device one.
 *
 * Re-asserted on every token the delegate reports, so a rotated or reinstalled token re-subscribes
 * without the user touching the toggle again.
 */
fun onFcmRegistrationTokenAvailable() {
    val koin = KoinPlatform.getKoin()
    if (!koin.get<AppSettings>().notificationsEnabled) return
    CoroutineScope(Dispatchers.Main).launch {
        koin.get<PushNotifications>().setSubscribed(true)
    }
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
