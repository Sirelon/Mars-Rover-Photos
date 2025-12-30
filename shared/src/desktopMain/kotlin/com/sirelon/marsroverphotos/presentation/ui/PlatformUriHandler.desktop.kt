package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.runtime.Composable
import java.awt.Desktop
import java.net.URI

/**
 * Desktop implementation using java.awt.Desktop.
 */
actual class PlatformUriHandler {
    actual fun openUri(uri: String) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI(uri))
            }
        } catch (e: Exception) {
            println("Failed to open URI: $uri - ${e.message}")
        }
    }
}

@Composable
actual fun rememberPlatformUriHandler(): PlatformUriHandler {
    return PlatformUriHandler()
}
