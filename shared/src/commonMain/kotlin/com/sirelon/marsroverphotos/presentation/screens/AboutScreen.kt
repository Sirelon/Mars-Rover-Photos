package com.sirelon.marsroverphotos.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import com.sirelon.marsroverphotos.domain.settings.Theme
import com.sirelon.marsroverphotos.presentation.navigation.LocalAboutCallbacks
import com.sirelon.marsroverphotos.presentation.ui.RadioButtonText
import com.sirelon.marsroverphotos.presentation.ui.rememberPlatformUriHandler
import com.sirelon.marsroverphotos.presentation.viewmodels.AboutViewModel
import com.sirelon.marsroverphotos.shared.resources.Res
import com.sirelon.marsroverphotos.shared.resources.alien_icon
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AboutScreen() {
    val viewModel: AboutViewModel = koinViewModel()
    val callbacks = LocalAboutCallbacks.current
    val coilContext = LocalPlatformContext.current
    AboutContent(
        viewModel = viewModel,
        onClearCache = {
            val loader = SingletonImageLoader.get(coilContext)
            loader.diskCache?.clear()
            loader.memoryCache?.clear()
        },
        onRateApp = callbacks.onRateApp,
        appVersion = callbacks.appVersion,
        rateAppUrl = callbacks.rateAppUrl
    )
}

@Composable
private fun AboutContent(
    viewModel: AboutViewModel,
    onClearCache: () -> Unit,
    onRateApp: () -> Unit,
    appVersion: String,
    rateAppUrl: String
) {
    val typography = MaterialTheme.typography
    val uriHandler = rememberPlatformUriHandler()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(Res.drawable.alien_icon),
            contentDescription = "logo"
        )
        Text(text = "Mars Rover Photos", style = typography.headlineSmall)
        Text(
            text = "Browse photos taken by NASA's Mars rovers. Explore the red planet through the eyes of Curiosity, Opportunity, Spirit, and Perseverance.",
            style = typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Column(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .fillMaxWidth()
        ) {
            LinkifyText(text = "API provided by ", link = "https://api.nasa.gov/")
            LinkifyText(text = "Email: ", link = "mailto:sasha.sirelon@gmail.com")
            if (appVersion.isNotEmpty()) {
                Text(
                    text = "Version: $appVersion",
                    modifier = Modifier.padding(4.dp)
                )
            }
        }

        ThemeChanger(viewModel)
        FactsToggle(viewModel)

        OutlinedButton(onClick = onClearCache) {
            Text(text = "Clear Cache")
        }

        Button(
            onClick = {
                if (rateAppUrl.isNotBlank()) {
                    uriHandler.openUri(rateAppUrl)
                } else {
                    onRateApp()
                }
            }
        ) {
            Text(text = "Rate App")
        }

        val currentYear = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).year
        Text(
            text = "© $currentYear All rights reserved.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun ThemeChanger(viewModel: AboutViewModel) {
    val currentTheme by viewModel.themeFlow.collectAsState()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Change the app theme",
                style = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RadioButtonText(text = "White", selected = currentTheme == Theme.WHITE) {
                    viewModel.setTheme(Theme.WHITE)
                }
                RadioButtonText(text = "Dark", selected = currentTheme == Theme.DARK) {
                    viewModel.setTheme(Theme.DARK)
                }
                RadioButtonText(text = "System", selected = currentTheme == Theme.SYSTEM) {
                    viewModel.setTheme(Theme.SYSTEM)
                }
            }
        }
    }
}

@Composable
private fun FactsToggle(viewModel: AboutViewModel) {
    val showFacts by viewModel.showFactsFlow.collectAsState()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Educational Facts",
                style = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center)
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                text = "Show \"Did You Know?\" fact cards while browsing photos.",
                style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RadioButtonText(text = "Show Facts", selected = showFacts) {
                    viewModel.toggleFacts(true)
                }
                RadioButtonText(text = "Hide Facts", selected = !showFacts) {
                    viewModel.toggleFacts(false)
                }
            }
        }
    }
}

@Composable
private fun LinkifyText(text: String, link: String) {
    val uriHandler = rememberPlatformUriHandler()
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    val colors = MaterialTheme.colorScheme

    val annotatedString = AnnotatedString.Builder().apply {
        append(text)
        pushStyle(
            style = SpanStyle(
                color = colors.primary,
                textDecoration = TextDecoration.Underline
            )
        )
        append(link)
        addStringAnnotation(
            tag = "URL",
            annotation = link,
            start = text.length,
            end = text.length + link.length
        )
    }.toAnnotatedString()

    val tapGesture = Modifier.pointerInput(null) {
        detectTapGestures { offset ->
            layoutResult.value?.let { result ->
                val position = result.getOffsetForPosition(offset)
                annotatedString.getStringAnnotations(position, position).firstOrNull()
                    ?.let { annotation ->
                        if (annotation.tag == "URL") {
                            uriHandler.openUri(annotation.item)
                        }
                    }
            }
        }
    }

    Text(
        text = annotatedString,
        modifier = Modifier
            .padding(4.dp)
            .then(tapGesture),
        onTextLayout = { layoutResult.value = it }
    )
}
