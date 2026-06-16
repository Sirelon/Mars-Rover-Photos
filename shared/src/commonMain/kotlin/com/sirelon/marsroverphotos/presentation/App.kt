package com.sirelon.marsroverphotos.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.memory.MemoryCache
import com.sirelon.marsroverphotos.domain.settings.AppSettings
import com.sirelon.marsroverphotos.domain.settings.Theme
import com.sirelon.marsroverphotos.presentation.navigation.AboutCallbacks
import com.sirelon.marsroverphotos.presentation.navigation.AppNavigation
import com.sirelon.marsroverphotos.presentation.navigation.DeepLink
import com.sirelon.marsroverphotos.presentation.navigation.LocalAboutCallbacks
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
    rateAppUrl: String = ""
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
