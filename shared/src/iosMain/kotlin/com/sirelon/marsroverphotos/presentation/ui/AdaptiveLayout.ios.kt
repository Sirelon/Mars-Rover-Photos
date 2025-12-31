package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.UIKit.UIScreen

/**
 * iOS implementation: Get screen width from UIScreen.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalForeignApi::class)
@Composable
actual fun getScreenWidthDp(): Float {
    val density = LocalDensity.current
    val screenBounds = UIScreen.mainScreen.nativeBounds
    val widthPixels = screenBounds.useContents { size.width }
    return with(density) { widthPixels.toFloat().toDp().value }
}

/**
 * iOS implementation: Get screen height from UIScreen.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalForeignApi::class)
@Composable
actual fun getScreenHeightDp(): Float {
    val density = LocalDensity.current
    val screenBounds = UIScreen.mainScreen.nativeBounds
    val heightPixels = screenBounds.useContents { size.height }
    return with(density) { heightPixels.toFloat().toDp().value }
}
