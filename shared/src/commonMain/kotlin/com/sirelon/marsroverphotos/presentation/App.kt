package com.sirelon.marsroverphotos.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.pointer.pointerInput
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.memory.MemoryCache
import com.sirelon.marsroverphotos.domain.settings.AppSettings
import com.sirelon.marsroverphotos.domain.settings.Theme
import com.sirelon.marsroverphotos.platform.BuildInfo
import com.sirelon.marsroverphotos.presentation.navigation.AboutCallbacks
import com.sirelon.marsroverphotos.presentation.navigation.AppNavigation
import com.sirelon.marsroverphotos.presentation.navigation.DeepLink
import com.sirelon.marsroverphotos.presentation.navigation.LocalAboutCallbacks
import com.sirelon.marsroverphotos.platform.PushNotifications
import com.sirelon.marsroverphotos.presentation.theme.AppSpacing
import com.sirelon.marsroverphotos.presentation.theme.MarsRoverPhotosTheme
import com.sirelon.marsroverphotos.presentation.theme.isSystemInDarkTheme
import com.sirelon.marsroverphotos.presentation.theme.supportsDynamicColor
import org.koin.compose.koinInject

/**
 * Main app composable.
 * Root of the Compose UI hierarchy.
 */
@Composable
fun App(
    deepLink: DeepLink? = null,
    onDeepLinkConsumed: (() -> Unit)? = null,
    onRateApp: () -> Unit = {},
    appVersion: String = "",
    rateAppUrl: String = "",
    debugLabel: String = ""
) {
    // Keep the in-memory image cache large enough that the fullscreen viewer's big bitmaps don't
    // evict the list/grid thumbnails. The list screens are disposed while the viewer is on top (a
    // normal Nav3 push — needed for the shared-element morph), so on return they recompose and
    // reload their images; with the thumbnails still cached this is instant (no placeholder flash).
    // Coil's default is ~25% of app memory.
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.40)
                    .build()
            }
            .build()
    }

    val appSettings: AppSettings = koinInject()
    val pushNotifications: PushNotifications = koinInject()

    // Re-assert the topic subscription on every launch. Topic membership is tied to the FCM
    // registration token, which a reinstall or a restore-from-backup replaces while the "enabled"
    // preference comes back — so without this the setting reads on and nothing ever arrives.
    // Idempotent, and it pairs with the per-launch APNs re-registration in the iOS app delegate:
    // that supplies the token this subscription needs.
    LaunchedEffect(Unit) {
        if (appSettings.notificationsEnabled) pushNotifications.setSubscribed(true)
    }

    val theme by appSettings.themeFlow.collectAsStateWithLifecycle()
    val systemDarkTheme = isSystemInDarkTheme()
    val dynamicColor = supportsDynamicColor()

    val useDarkTheme = when (theme) {
        Theme.DARK -> true
        Theme.WHITE -> false
        Theme.SYSTEM -> systemDarkTheme
    }

    MarsRoverPhotosTheme(
        darkTheme = useDarkTheme,
        dynamicColor = dynamicColor
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                CompositionLocalProvider(
                    LocalAboutCallbacks provides AboutCallbacks(
                        onRateApp = onRateApp,
                        appVersion = appVersion,
                        rateAppUrl = rateAppUrl
                    )
                ) {
                    AppContent(
                        deepLink = deepLink,
                        onDeepLinkConsumed = onDeepLinkConsumed
                    )
                }
                if (BuildInfo.isDebug && debugLabel.isNotBlank()) {
                    DebugBuildLabel(
                        label = debugLabel,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppContent(
    deepLink: DeepLink?,
    onDeepLinkConsumed: (() -> Unit)?
) {
    AppNavigation(
        modifier = Modifier.fillMaxSize(),
        deepLink = deepLink,
        onDeepLinkConsumed = onDeepLinkConsumed
    )
}

@Composable
private fun DebugBuildLabel(label: String, modifier: Modifier = Modifier) {
    val bgColor = MaterialTheme.colorScheme.errorContainer
    val textColor = MaterialTheme.colorScheme.onErrorContainer
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = textColor,
        maxLines = 1,
        modifier = modifier
            .statusBarsPadding()
            .padding(end = AppSpacing.sm, top = AppSpacing.xs)
            .pointerInput(Unit) {}
            .clip(CircleShape)
            .drawBehind { drawRect(bgColor.copy(alpha = 0.9f)) }
            .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs)
    )
}
