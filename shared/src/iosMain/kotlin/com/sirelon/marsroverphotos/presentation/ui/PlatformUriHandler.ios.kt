package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.runtime.Composable
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

/**
 * iOS implementation - TODO: Implement with iOS native URL opening.
 */
actual class PlatformUriHandler {
    actual fun openUri(uri: String) {
        val url = NSURL.URLWithString(uri)
        if (url == null) {
            println("Invalid URI: $uri")
            return
        }
        UIApplication.sharedApplication.openURL(url)
    }
}

@Composable
actual fun rememberPlatformUriHandler(): PlatformUriHandler {
    return PlatformUriHandler()
}
