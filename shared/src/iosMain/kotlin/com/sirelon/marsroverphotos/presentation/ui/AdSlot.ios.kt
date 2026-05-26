package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.sirelon.marsroverphotos.platform.IosAdSlot

@Composable
actual fun AdSlot(modifier: Modifier) {
    val factory = IosAdSlot.factory ?: return
    BoxWithConstraints(modifier = modifier) {
        val widthDp = maxWidth.value.toDouble()
        UIKitView(
            factory = {
                factory.createBanner(widthDp)
            },
            modifier = Modifier.fillMaxWidth(),
            update = { },
            onRelease = { },
            properties = UIKitInteropProperties(
                isInteractive = true,
                isNativeAccessibilityEnabled = true
            ),
        )
    }
}
