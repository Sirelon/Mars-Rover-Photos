package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * A slim banner composable that displays the Ukrainian flag colors and a title.
 * Tapping the banner invokes [onClick]; tapping the close icon invokes [onDismiss].
 */
@Composable
fun UkraineBanner(
    modifier: Modifier = Modifier,
    title: String = "#Stand with Ukraine",
    onClick: () -> Unit,
    onDismiss: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 30.dp)
            .clickable(onClick = onClick)
            .drawBehind {
                val halfHeight = size.height / 2
                val alpha = 0.5f
                drawRect(
                    color = Color.Blue.copy(alpha = alpha),
                    size = size.copy(height = halfHeight),
                )
                drawRect(
                    color = Color.Yellow.copy(alpha = alpha),
                    size = size.copy(height = halfHeight),
                    topLeft = Offset(x = 0f, y = halfHeight),
                )
            },
    ) {
        Text(
            text = title,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 40.dp, vertical = 4.dp),
            textAlign = TextAlign.Center,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge.copy(
                shadow = Shadow(
                    color = Color.Black,
                    offset = Offset(2f, 2f),
                    blurRadius = 2f,
                ),
            ),
        )

        if (onDismiss != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
                    .size(28.dp)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                MaterialSymbolIcon(
                    symbol = MaterialSymbol.Close,
                    contentDescription = "Dismiss Ukraine banner",
                    tint = Color.White,
                    size = 18.dp,
                )
            }
        }
    }
}
