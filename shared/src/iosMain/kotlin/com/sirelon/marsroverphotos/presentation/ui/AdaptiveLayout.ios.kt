package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import platform.UIKit.UIScreen

/**
 * iOS implementation: Get screen width from UIScreen.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun getScreenWidthDp(): Float {
    val density = LocalDensity.current
    val screenBounds = UIScreen.mainScreen.bounds
    val widthPixels = screenBounds.useContents { size.width }
    return with(density) { widthPixels.toFloat().toDp().value }
}

/**
 * iOS implementation: Get screen height from UIScreen.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun getScreenHeightDp(): Float {
    val density = LocalDensity.current
    val screenBounds = UIScreen.mainScreen.bounds
    val heightPixels = screenBounds.useContents { size.height }
    return with(density) { heightPixels.toFloat().toDp().value }
}
