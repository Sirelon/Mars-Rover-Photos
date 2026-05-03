package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Android implementation: Get screen width from LocalConfiguration.
 */
@Composable
actual fun getScreenWidthDp(): Float {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp.toFloat()
}

/**
 * Android implementation: Get screen height from LocalConfiguration.
 */
@Composable
actual fun getScreenHeightDp(): Float {
    val configuration = LocalConfiguration.current
    return configuration.screenHeightDp.toFloat()
}
