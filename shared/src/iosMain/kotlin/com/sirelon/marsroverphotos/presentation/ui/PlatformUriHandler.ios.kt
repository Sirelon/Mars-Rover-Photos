package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.runtime.Composable

/**
 * iOS implementation - TODO: Implement with iOS native URL opening.
 */
actual class PlatformUriHandler {
    actual fun openUri(uri: String) {
        // TODO: Implement with iOS UIApplication.shared.open()
        println("Open URI on iOS: $uri")
    }
}

@Composable
actual fun rememberPlatformUriHandler(): PlatformUriHandler {
    return PlatformUriHandler()
}
