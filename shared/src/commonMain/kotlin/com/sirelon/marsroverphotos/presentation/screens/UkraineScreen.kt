package com.sirelon.marsroverphotos.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.sirelon.marsroverphotos.platform.Tracker
import com.sirelon.marsroverphotos.platform.recordException
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbol
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbolIcon
import com.sirelon.marsroverphotos.presentation.ui.UkraineBanner
import com.sirelon.marsroverphotos.presentation.ui.rememberPlatformUriHandler
import org.koin.compose.koinInject

/**
 * Full-screen Ukraine info screen. Describes the current situation in Ukraine and
 * provides outbound links to external resources.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UkraineScreen(onBack: () -> Unit) {
    val uriHandler = rememberPlatformUriHandler()
    val tracker: Tracker = koinInject()

    fun openUri(uri: String) {
        try {
            uriHandler.openUri(uri)
        } catch (e: Throwable) {
            recordException(e)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(),
        topBar = {
            TopAppBar(
                title = { Text("Ukraine") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        MaterialSymbolIcon(
                            symbol = MaterialSymbol.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.titleMedium.copy(
                    textAlign = TextAlign.Center,
                ),
            ) {
                UkraineInfoContent(
                    onTrackClick = { event -> tracker.trackClick(event) },
                    onOpenUri = ::openUri,
                )
            }

            UkraineBanner(
                modifier = Modifier.fillMaxWidth(),
                title = "Glory to Ukraine",
                onClick = {
                    tracker.trackClick("UkraineBanner_Bottom")
                    openUri("https://twitter.com/search?q=%23StandWithUkraine")
                },
            )
        }
    }
}

@Composable
private fun UkraineInfoContent(
    onTrackClick: (String) -> Unit,
    onOpenUri: (String) -> Unit,
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Hello, I'm Oleksandr, a proud Ukrainian",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "As you may be aware, Ukraine is currently facing a severe and merciless war. " +
                "Countless lives have been lost, and our cities endure daily missile strikes, " +
                "with some territories occupied and the true extent of casualties unknown.",
        )
        Text(
            text = "In a world where Russia, without a shred of decency, prioritizes war over " +
                "science, engages in daily violence, and propagates hatred, it has even spawned " +
                "a new form of fascism called",
        )
        TextButton(
            onClick = {
                onTrackClick("Ukraine_wiki")
                onOpenUri("https://en.wikipedia.org/wiki/Rashism")
            },
        ) {
            Text(text = "rashism.", textDecoration = TextDecoration.Underline)
        }
        Text(
            text = "The sheer brutality of it is unfathomable in the 21st century. Yet, Ukraine " +
                "persists against this evil, despite being outgunned and outmanned by Russia. " +
                "We're not just surviving; we're fighting tooth and nail to liberate our territory " +
                "and people.",
        )
        Text(
            text = "This struggle is nothing short of incredible, fueled by the support of " +
                "compassionate individuals worldwide. Massive thanks to those who stand with us " +
                "in this desperate battle for survival.",
        )
        Text(
            text = "Thank you!",
            color = Color.Red,
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = "Don't just scroll past this. Remember us. We need your unwavering support as " +
                "the war rages on. We're fighting for democracy, for our lives, and for a better " +
                "future for everyone.",
        )
        Text(text = "For more gritty details and stories of resilience from Ukrainian heroes, check out")
        TextButton(
            onClick = {
                onTrackClick("Ukraine_site")
                onOpenUri("https://war.ukraine.ua/")
            },
        ) {
            Text(text = "https://war.ukraine.ua/", textDecoration = TextDecoration.Underline)
        }
        Text(text = "If you have any questions, reach me via email")
        TextButton(
            onClick = {
                onTrackClick("Ukraine_mail")
                onOpenUri("mailto:sasha.sirelon@gmail.com")
            },
        ) {
            Text(text = "sasha.sirelon@gmail.com", textDecoration = TextDecoration.Underline)
        }
        Text(
            text = "Thanks a bunch – we're all in this together!",
            color = Color.Red,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}
