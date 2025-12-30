package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import kotlinx.browser.window

/**
 * Web implementation: Get screen width from browser window.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun getScreenWidthDp(): Float {
    val density = LocalDensity.current
    val widthPixels = window.innerWidth.toFloat()
    return with(density) { widthPixels.toDp().value }
}

/**
 * Web implementation: Get screen height from browser window.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun getScreenHeightDp(): Float {
    val density = LocalDensity.current
    val heightPixels = window.innerHeight.toFloat()
    return with(density) { heightPixels.toDp().value }
}
