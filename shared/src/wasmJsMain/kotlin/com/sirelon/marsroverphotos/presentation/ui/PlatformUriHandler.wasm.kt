package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.runtime.Composable
import kotlinx.browser.window

/**
 * Web implementation using window.open().
 */
actual class PlatformUriHandler {
    actual fun openUri(uri: String) {
        window.open(uri, "_blank")
    }
}

@Composable
actual fun rememberPlatformUriHandler(): PlatformUriHandler {
    return PlatformUriHandler()
}
