package com.sirelon.marsroverphotos

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.sirelon.marsroverphotos.gdpr.GdprHelper
import com.sirelon.marsroverphotos.platform.ActivityProvider
import com.sirelon.marsroverphotos.platform.BuildInfo
import com.sirelon.marsroverphotos.presentation.App
import com.sirelon.marsroverphotos.presentation.navigation.DeepLink
import com.sirelon.marsroverphotos.presentation.navigation.parseDeepLink
import com.sirelon.marsroverphotos.utils.Logger
import com.sirelon.marsroverphotos.widget.WidgetExtraImageId

/**
 * Main activity for the Mars Rover Photos app.
 * Hosts the Compose UI from the shared module.
 */
class MainActivity : ComponentActivity() {
    private var pendingDeepLink: DeepLink? by mutableStateOf(null)
    private val gdprHelper = GdprHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install Android 12+ splash screen
        installSplashScreen()

        super.onCreate(savedInstanceState)
        // Register this Activity so shared-module code (e.g. Play in-app review)
        // can reach an Activity reference without leaking it.
        ActivityProvider.set(this)
        // Screenshot capture passes `hideAds` (Maestro launch argument → intent extra) to hide all ads.
        // Maestro forwards it as a string extra, so accept either a boolean or "true".
        val hideAds = intent?.getBooleanExtra("hideAds", false) == true ||
            intent?.getStringExtra("hideAds")?.equals("true", ignoreCase = true) == true
        if (hideAds) BuildInfo.hideAds = true
        enableEdgeToEdge()
        // Prevent the system from adding a translucent scrim over the NavigationBar —
        // the app's NavigationBar background provides the necessary contrast.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        gdprHelper.init()

        // Handle deep link if present
        handleDeepLink(intent)

        setContent {
            // Expose Compose testTags as resource-ids so UI-test tooling (Maestro) can target
            // elements by id. Propagates to all descendants; negligible runtime cost.
            @OptIn(ExperimentalComposeUiApi::class)
            Box(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
                App(
                    deepLink = pendingDeepLink,
                    onDeepLinkConsumed = { pendingDeepLink = null },
                    onRateApp = ::openStoreListing,
                    appVersion = BuildConfig.VERSION_NAME,
                    rateAppUrl = "https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}"
                )
            }
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

    override fun onDestroy() {
        ActivityProvider.clear(this)
        super.onDestroy()
    }

    /**
     * Resolves a launch [Intent] into a [DeepLink], if it carries one.
     *
     * Two sources: the home-screen widget's image extra, and the VIEW-intent URI declared by the
     * manifest's intent filters. URI forms are parsed by the shared [parseDeepLink] so Android and
     * iOS stay in step.
     */
    private fun handleDeepLink(intent: Intent?) {
        if (intent == null) return

        // Widget taps carry an image id rather than a URI — no public URI form exists for them.
        val widgetImageId = intent.getStringExtra(WidgetExtraImageId)
        if (!widgetImageId.isNullOrBlank()) {
            Logger.d(TAG) { "Widget deep link received: $widgetImageId" }
            pendingDeepLink = DeepLink.Image(widgetImageId)
            return
        }

        val uri = intent.data?.toString()
        if (uri == null) {
            Logger.d(TAG) { "No deep link data" }
            return
        }

        val parsed = parseDeepLink(uri)
        if (parsed == null) {
            Logger.w(TAG) { "Unrecognised deep link: $uri" }
            return
        }
        Logger.d(TAG) { "Deep link received: $uri" }
        pendingDeepLink = parsed
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}
