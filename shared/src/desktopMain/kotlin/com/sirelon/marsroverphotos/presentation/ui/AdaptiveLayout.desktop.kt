package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import java.awt.Toolkit

/**
 * Desktop implementation: Get screen width from AWT Toolkit.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun getScreenWidthDp(): Float {
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current

    // Try to get window size first, fall back to screen size
    val widthPixels = if (windowInfo.containerSize.width > 0) {
        windowInfo.containerSize.width.toFloat()
    } else {
        val screenSize = Toolkit.getDefaultToolkit().screenSize
        screenSize.width.toFloat()
    }

    return with(density) { widthPixels.toDp().value }
}

/**
 * Desktop implementation: Get screen height from AWT Toolkit.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun getScreenHeightDp(): Float {
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current

    // Try to get window size first, fall back to screen size
    val heightPixels = if (windowInfo.containerSize.height > 0) {
        windowInfo.containerSize.height.toFloat()
    } else {
        val screenSize = Toolkit.getDefaultToolkit().screenSize
        screenSize.height.toFloat()
    }

    return with(density) { heightPixels.toDp().value }
}
