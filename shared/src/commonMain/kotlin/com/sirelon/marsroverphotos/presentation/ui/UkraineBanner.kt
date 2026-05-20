package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * A banner composable that displays the Ukrainian flag colors and a title.
 * Tapping the banner invokes [onClick].
 */
@Composable
fun UkraineBanner(
    modifier: Modifier = Modifier,
    title: String = "#Stand with Ukraine",
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
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
                .padding(8.dp),
            textAlign = TextAlign.Center,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge.copy(
                shadow = Shadow(
                    color = Color.Black,
                    offset = Offset(2f, 2f),
                    blurRadius = 2f,
                ),
            ),
        )
    }
}
