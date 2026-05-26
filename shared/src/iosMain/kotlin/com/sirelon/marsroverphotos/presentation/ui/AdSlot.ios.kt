package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.sirelon.marsroverphotos.platform.IosAdSlot
import platform.UIKit.UIView

private val BannerMinHeight = 50.dp

@Composable
actual fun AdSlot(modifier: Modifier) {
    BoxWithConstraints(modifier = modifier) {
        val widthDp = maxWidth.value.toDouble()
        UIKitView(
            factory = {
                IosAdSlot.factory?.createBanner(widthDp) ?: UIView()
            },
            modifier = Modifier.fillMaxWidth().height(BannerMinHeight),
            update = { },
            onRelease = { },
            properties = UIKitInteropProperties(
                isInteractive = true,
                isNativeAccessibilityEnabled = true
            ),
        )
    }
}
