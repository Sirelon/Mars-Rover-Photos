package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.runtime.Composable
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

/**
 * iOS implementation using UIApplication to open URLs natively.
 *
 * Uses the modern `openURL(_:options:completionHandler:)` API (iOS 10+) instead of the
 * deprecated single-argument `openURL(_:)`.  Both http(s) and mailto: schemes are
 * handled by the OS — if no app can handle the URL the completion handler receives false.
 */
actual class PlatformUriHandler {
    actual fun openUri(uri: String) {
        val url = NSURL.URLWithString(uri)
        if (url == null) {
            println("PlatformUriHandler: invalid URI skipped: $uri")
            return
        }
        // Modern API: options dictionary is empty (no need for UIApplicationOpenExternalURLOptionsKey
        // values for plain browser / mail links). completionHandler logs failures for debugging.
        UIApplication.sharedApplication.openURL(
            url = url,
            options = emptyMap<Any?, Any?>(),
            completionHandler = { success ->
                if (!success) println("PlatformUriHandler: OS could not open URI: $uri")
            }
        )
    }
}

@Composable
actual fun rememberPlatformUriHandler(): PlatformUriHandler {
    return PlatformUriHandler()
}
