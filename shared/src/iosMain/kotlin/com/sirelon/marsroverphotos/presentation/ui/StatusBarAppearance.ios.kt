package com.sirelon.marsroverphotos.presentation.ui

import platform.UIKit.UIApplication
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIWindowScene

/**
 * iOS implementation — overrides the key window's `userInterfaceStyle` so that
 * dark-mode rendering (= white status-bar icons) is forced while the fullscreen
 * photo pager is visible, then restored to the system default on dispose.
 *
 * `UIUserInterfaceStyleDark`       → light (white) status-bar icons
 * `UIUserInterfaceStyleUnspecified` → follows the system dark/light preference
 */
actual fun setStatusBarAppearance(lightIcons: Boolean) {
    val style = if (lightIcons) {
        UIUserInterfaceStyle.UIUserInterfaceStyleDark
    } else {
        UIUserInterfaceStyle.UIUserInterfaceStyleUnspecified
    }

    val scene = UIApplication.sharedApplication.connectedScenes
        .asSequence()
        .mapNotNull { it as? UIWindowScene }
        .firstOrNull() ?: return

    scene.windows.forEach { window ->
        (window as? platform.UIKit.UIWindow)?.overrideUserInterfaceStyle = style
    }
}
