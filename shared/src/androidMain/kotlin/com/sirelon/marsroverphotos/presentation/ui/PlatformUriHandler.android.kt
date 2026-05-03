package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler

/**
 * Android implementation using Compose's LocalUriHandler.
 */
actual class PlatformUriHandler(
    private val uriHandler: androidx.compose.ui.platform.UriHandler
) {
    actual fun openUri(uri: String) {
        uriHandler.openUri(uri)
    }
}

@Composable
actual fun rememberPlatformUriHandler(): PlatformUriHandler {
    return PlatformUriHandler(LocalUriHandler.current)
}
