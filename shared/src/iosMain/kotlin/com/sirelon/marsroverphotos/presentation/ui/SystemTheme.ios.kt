package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.runtime.Composable
import platform.UIKit.UIScreen
import platform.UIKit.UIUserInterfaceStyle

@Composable
actual fun isSystemInDarkTheme(): Boolean {
    return UIScreen.mainScreen.traitCollection.userInterfaceStyle == UIUserInterfaceStyle.UIUserInterfaceStyleDark
}
