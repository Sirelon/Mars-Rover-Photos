package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.runtime.Composable

/**
 * Platform-agnostic URI handler for opening external links.
 */
expect class PlatformUriHandler {
    /**
     * Open a URI (URL, email, etc.) using the platform's default handler.
     */
    fun openUri(uri: String)
}

/**
 * Get a platform URI handler.
 */
@Composable
expect fun rememberPlatformUriHandler(): PlatformUriHandler
