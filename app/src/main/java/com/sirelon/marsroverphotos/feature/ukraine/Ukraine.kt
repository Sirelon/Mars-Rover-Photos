package com.sirelon.marsroverphotos.feature.ukraine

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sirelon.marsroverphotos.RoverApplication
import com.sirelon.marsroverphotos.extensions.recordException

@Composable
@Preview(showBackground = true, device = Devices.DEFAULT)
fun UkraineInfoScreen() {
    val uriHandler = LocalUriHandler.current

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.subtitle1.copy(
                textAlign = TextAlign.Center
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Hello, I'm Oleksandr, a proud Ukrainian",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.subtitle2,
                )
                Text(text = "As you may be aware, Ukraine is currently facing a severe and merciless war. Countless lives have been lost, and our cities endure daily missile strikes, with some territories occupied and the true extent of casualties unknown.")
                Text(text = "In a world where Russia, without a shred of decency, prioritizes war over science, engaging in daily violence and propagating hatred, they've even birthed a new form of fascism called")

                TextButton(
                    onClick = {
                        RoverApplication.APP.tracker.trackClick("Ukraine_wiki")
                        uriHandler.openUri("https://en.wikipedia.org/wiki/Rashism")
                    }) {
                    Text(text = "rashism.")
                }
                Text(text = "The sheer brutality of it is unfathomable in the 21st century. Yet, Ukraine persists against this evil, despite being outgunned and outmanned by Russia. We're not just surviving; we're fighting tooth and nail to liberate our territory and people.")
                Text(text = "This struggle is nothing short of incredible, fueled by the support of compassionate individuals worldwide. Massive thanks to those who stand with us in this desperate battle for survival.")
                Text(
                    text = "Thank you!",
                    color = Color.Red,
                    style = MaterialTheme.typography.subtitle2
                )
                Text(text = "Don't just scroll past this. Remember us. We need your unwavering support as the war rages on. We're fighting for democracy, for our lives, and for a better future for everyone.")
                Text(text = "For more gritty details and stories of resilience from Ukrainian heroes, check out ")
                TextButton(onClick = {
                    RoverApplication.APP.tracker.trackClick("Ukraine_site")
                    uriHandler.openUri("https://war.ukraine.ua/")
                }) {
                    Text(text = "https://war.ukraine.ua/")
                }
                Text(text = "If you have any question, reach me via email ")

                TextButton(onClick = {
                    RoverApplication.APP.tracker.trackClick("Ukraine_mail")
                    uriHandler.openUri("mailto:sasha.sirelon@gmail.com")
                }) {
                    Text(text = "sasha.sirelon@gmail.com")
                }
                Text(
                    text = "Thanks a bunch – we're all in this together!",
                    color = Color.Red,
                    style = MaterialTheme.typography.subtitle2
                )
            }
        }

        UkraineBanner(title = "Glory to Ukraine") {
            RoverApplication.APP.tracker.trackClick("UkraineBanner_Bottom")
            // Open twitter
            try {
                uriHandler.openUri("twitter://search?query=%23StandWithUkraine")
            } catch (e: Throwable) {
                recordException(e)
            }

        }
    }
}

@Composable
fun UkraineBanner(
    modifier: Modifier = Modifier,
    title: String = "#Stand with Ukraine",
    onClick: () -> Unit
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
                    size = size.copy(height = halfHeight)
                )
                drawRect(
                    color = Color.Yellow.copy(alpha = alpha),
                    size = size.copy(height = halfHeight),
                    topLeft = Offset(x = 0f, y = halfHeight)
                )
            }
    ) {
        Text(
            text = title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            textAlign = TextAlign.Center,
            color = Color.White,
            style = MaterialTheme.typography.h6.copy(
                shadow = Shadow(
                    color = Color.Black,
                    offset = Offset(2f, 2f),
                    blurRadius = 2f
                )
            )
        )
    }
}

@Preview(showBackground = true, device = Devices.DEFAULT)
@Composable
private fun PreviewBanner() {
    UkraineBanner {

    }
}