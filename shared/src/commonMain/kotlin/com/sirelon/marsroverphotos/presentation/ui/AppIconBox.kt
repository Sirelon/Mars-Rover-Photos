package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.sirelon.marsroverphotos.presentation.theme.AppSize

/**
 * Design-system tinted icon-box — a rounded square ([container] fill) holding a [tint]ed
 * [MaterialSymbolIcon]. A general leading visual for list rows, settings rows, feature tiles, etc.
 */
@Composable
fun AppIconBox(
    symbol: MaterialSymbol,
    container: Color,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(AppSize.iconBox)
            .clip(RoundedCornerShape(AppSize.iconBoxRadius))
            .background(container),
        contentAlignment = Alignment.Center
    ) {
        MaterialSymbolIcon(symbol = symbol, contentDescription = null, tint = tint, size = AppSize.icon)
    }
}
