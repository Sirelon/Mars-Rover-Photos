package com.sirelon.marsroverphotos.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.sirelon.marsroverphotos.presentation.theme.AppSpacing
import com.sirelon.marsroverphotos.presentation.theme.AppTypography
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import com.sirelon.marsroverphotos.domain.settings.Theme
import com.sirelon.marsroverphotos.platform.AppReview
import com.sirelon.marsroverphotos.presentation.navigation.LocalAboutCallbacks
import com.sirelon.marsroverphotos.presentation.ui.AppButton
import com.sirelon.marsroverphotos.presentation.ui.AppCard
import com.sirelon.marsroverphotos.presentation.ui.AppOutlinedButton
import com.sirelon.marsroverphotos.presentation.ui.MarsSnackbar
import com.sirelon.marsroverphotos.presentation.ui.RadioButtonText
import com.sirelon.marsroverphotos.presentation.ui.rememberPlatformUriHandler
import com.sirelon.marsroverphotos.presentation.viewmodels.AboutViewModel
import kotlinx.coroutines.launch
import com.sirelon.marsroverphotos.shared.resources.Res
import com.sirelon.marsroverphotos.shared.resources.alien_icon
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AboutScreen() {
    val viewModel: AboutViewModel = koinViewModel()
    val callbacks = LocalAboutCallbacks.current
    val coilContext = LocalPlatformContext.current
    val currentTheme by viewModel.themeFlow.collectAsStateWithLifecycle()
    val showFacts by viewModel.showFactsFlow.collectAsStateWithLifecycle()
    AboutContent(
        currentTheme = currentTheme,
        showFacts = showFacts,
        onThemeChange = viewModel::setTheme,
        onFactsToggle = viewModel::toggleFacts,
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
    currentTheme: Theme,
    showFacts: Boolean,
    onThemeChange: (Theme) -> Unit,
    onFactsToggle: (Boolean) -> Unit,
    onClearCache: () -> Unit,
    onRateApp: () -> Unit,
    appVersion: String,
    rateAppUrl: String
) {
    val uriHandler = rememberPlatformUriHandler()
    val appReview: AppReview = koinInject()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppSpacing.lg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(Res.drawable.alien_icon),
                contentDescription = "logo"
            )
            Text(text = "Mars Rover Photos", style = AppTypography.appTitle)
            Text(
                text = "Browse photos taken by NASA's Mars rovers. Explore the red planet through the eyes of Curiosity, Opportunity, Spirit, Perseverance, and InSight.",
                style = AppTypography.body,
                textAlign = TextAlign.Center
            )

            Column(
                modifier = Modifier
                    .padding(vertical = AppSpacing.sm)
                    .fillMaxWidth()
            ) {
                LinkifyText(text = "API provided by ", link = "https://api.nasa.gov/")
                LinkifyText(
                    text = "Email: ",
                    link = "mailto:sasha.sirelon@gmail.com",
                    displayLink = "sasha.sirelon@gmail.com"
                )
                if (appVersion.isNotEmpty()) {
                    Text(
                        text = "Version: $appVersion",
                        modifier = Modifier.padding(AppSpacing.xs)
                    )
                }
            }

            ThemeChanger(currentTheme = currentTheme, onThemeChange = onThemeChange)
            FactsToggle(showFacts = showFacts, onFactsToggle = onFactsToggle)

            AppOutlinedButton(onClick = {
                onClearCache()
                scope.launch { snackbarHostState.showSnackbar("Cache cleared") }
            }) {
                Text(text = "Clear Cache")
            }

            AppButton(
                onClick = {
                    scope.launch {
                        val shown = appReview.requestReview()
                        if (!shown) {
                            if (rateAppUrl.isNotBlank()) {
                                uriHandler.openUri(rateAppUrl)
                            } else {
                                onRateApp()
                            }
                        }
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
                modifier = Modifier.padding(AppSpacing.lg)
            )
        }

        MarsSnackbar(
            snackbarHostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
            actionClick = null
        )
    }
}

@Composable
private fun ThemeChanger(currentTheme: Theme, onThemeChange: (Theme) -> Unit) {
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.sm),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppSpacing.lg)
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Change the app theme",
                style = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center)
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RadioButtonText(selected = currentTheme == Theme.WHITE, onClick = { onThemeChange(Theme.WHITE) }) {
                    Text("White")
                }
                RadioButtonText(selected = currentTheme == Theme.DARK, onClick = { onThemeChange(Theme.DARK) }) {
                    Text("Dark")
                }
                RadioButtonText(selected = currentTheme == Theme.SYSTEM, onClick = { onThemeChange(Theme.SYSTEM) }) {
                    Text("System")
                }
            }
        }
    }
}

@Composable
private fun FactsToggle(showFacts: Boolean, onFactsToggle: (Boolean) -> Unit) {
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.sm),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Educational Facts",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Show \"Did You Know?\" fact cards while browsing photos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = showFacts,
                onCheckedChange = onFactsToggle
            )
        }
    }
}

/**
 * Shows a label followed by a tappable hyperlink.
 *
 * Uses [Modifier.clickable] on the link portion rather than [detectTapGestures] so the
 * tap reliably fires on iOS inside a vertically scrollable container.
 */
@Composable
private fun LinkifyText(text: String, link: String, displayLink: String = link) {
    val uriHandler = rememberPlatformUriHandler()
    val textStyle = LocalTextStyle.current
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.padding(AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (text.isNotEmpty()) {
            Text(text = text, style = textStyle)
        }
        Text(
            text = displayLink,
            style = textStyle.copy(textDecoration = TextDecoration.Underline),
            color = colors.primary,
            modifier = Modifier.clickable { uriHandler.openUri(link) }
        )
    }
}
