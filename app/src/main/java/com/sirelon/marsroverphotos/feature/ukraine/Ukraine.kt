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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.sirelon.marsroverphotos.RoverApplication
import com.sirelon.marsroverphotos.extensions.recordException

@Composable
@Preview
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
                    text = "Hello, my name is Alex. I am Ukrainian, and I'm proud of it.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.subtitle2,
                )
                Text(text = "As you probably know, we have war in the Ukraine.. The very terrible and merciless war.")
                Text(text = "Many victims, many misfortunes, many broken lives are in this war. Everyday, during more than six month, our civilian cities is under attack of missile strikes, some territory are occupied and no one knows how many deaths are there.")
                Text(text = "In the world, where Elon Musk has plans to colonize the Mars, where NASA wants to resume mission to the Moon, where scientists from around the World united to take picture of Black Hole, in this world we have the country and people which start and support the biggest war after WW2 in Europe.")
                Text(text = "Russia, which doesn't want to invest money to science but invest to war. Russia, which is killing people everyday. Russia invest money to weapons, to propaganda, to hatred of you, if you are not a russian. ")
                TextButton(
                    onClick = {
                        RoverApplication.APP.tracker.trackClick("Ukraine_wiki")
                        uriHandler.openUri("https://en.wikipedia.org/wiki/Rashism")
                    }) {
                    Text(text = "Russia introduced new form of fascism: rashism. ")
                }
                Text(text = "It's terrible, and I don't understand how this could even happen in XXI century. ")
                Text(text = "But Ukraine are fighting with this evil. It's very hard, we don't have such resources as Russia has. But we stand. Event more, we are trying to liberate our territory and people. ")
                Text(text = "To be honest, it's fantastic. But without help from almost all world it couldn't be true. And I'm thank you all, who help us with our battle for life. ")
                Text(
                    text = "Thank you!",
                    color = Color.Red,
                    style = MaterialTheme.typography.subtitle2
                )
                Text(text = "But also I have to ask you something. Please, do not forget us. We need your support. The war hasn't ended. Remember that we are fighting with evil for democracy for our life for better future for all of us and you. ")
                Text(text = "For more information and for incredible stories about ukrainian heroes you can read here:")
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
                    text = "Thank you!",
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
fun UkraineBanner(title: String = "#Stand with Ukraine", onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .drawBehind {
                val halfHeight = size.height / 2
                drawRect(
                    color = Color.Blue,
                    size = size.copy(height = halfHeight)
                )
                drawRect(
                    color = Color.Yellow,
                    size = size.copy(height = halfHeight),
                    topLeft = Offset(x = 0f, y = halfHeight)
                )
            }
            .shadow(elevation = 2.dp)
    ) {
        Text(
            text = title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            textAlign = TextAlign.Center,
            color = Color.Red,
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