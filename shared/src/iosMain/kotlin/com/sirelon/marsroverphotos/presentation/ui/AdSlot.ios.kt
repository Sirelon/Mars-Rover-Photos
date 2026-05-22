@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import com.sirelon.marsroverphotos.platform.IosAdSlot

private val BannerMinHeight = 50.dp

@Composable
actual fun AdSlot(modifier: Modifier) {
    val factory = IosAdSlot.factory
    if (factory == null) {
        Box(modifier = modifier)
        return
    }

    BoxWithConstraints(modifier = modifier) {
        val widthDp = maxWidth.value.toDouble()
        UIKitView(
            factory = { factory.createBanner(widthDp) },
            modifier = Modifier.fillMaxWidth().height(BannerMinHeight)
        )
    }
}
