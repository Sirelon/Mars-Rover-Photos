package com.sirelon.marsroverphotos.feature.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sirelon.marsroverphotos.BuildConfig
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.RoverApplication
import com.sirelon.marsroverphotos.storage.Prefs
import com.sirelon.marsroverphotos.storage.Theme
import com.sirelon.marsroverphotos.ui.MarsRoverPhotosTheme
import com.sirelon.marsroverphotos.ui.RadioButtonText
import java.util.*

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MarsRoverPhotosTheme {
        Scaffold() {
            AboutAppContent({}, {})
        }
    }
}

@Composable
fun AboutAppContent(onClearCache: () -> Unit, onRateApp: () -> Unit) {
    val typography = MaterialTheme.typography
    val colors = MaterialTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(
                rememberScrollState()
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(painter = painterResource(id = R.drawable.alien_icon), contentDescription = "logo")
        Text(text = "Mars rover photos", style = typography.h5)
        Text(
            text = stringResource(id = R.string.about_description),
            style = typography.body1,
            textAlign = TextAlign.Center,
            color = colors.secondaryVariant
        )

        ThemeChanger()

        Divider()
        BundleSection()
        Divider()

        OutlinedButton(onClick = onClearCache) {
            Text(text = stringResource(id = R.string.clear_cache))
        }

        Button(onClick = onRateApp) {
            Text(text = stringResource(id = R.string.action_rate))
        }

        InfoSection()

        val copyrightText = stringResource(
            R.string.all_rights_reserved_fmt,
            Calendar.getInstance().get(Calendar.YEAR)
        )
        Text(
            text = copyrightText,
            style = MaterialTheme.typography.caption,
            modifier = Modifier.padding(16.dp)
        )

    }

}

data class BundleUi(
    val emoji: String,
    val title: String,
    val description: String,
    val selected: Boolean
)

@Composable
fun BundleSection() {
    Column {
        Title(title = "Remove ads and support me")

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = "Make a purchase and have fun without any ads!\nThis is not a subscription, you pay only once!\nPurchases secured with Google Play.",
            style = MaterialTheme.typography.body2,
            textAlign = TextAlign.Center,
        )

        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val bundles = listOf(
                BundleUi("🔥", "All life!", "Buy once, use all life!", false),
                BundleUi("✨", "For a month", "Remove ad for a month!", false),
            )

            bundles.forEachIndexed { index, bundle ->
                Card(
                    modifier = Modifier
//                        .wrapContentHeight(align = Alignment.CenterVertically)
                        .fillMaxWidth((index + 1f) / bundles.size)
                ) {
                    Column(
                        modifier = Modifier
                            .clickable { }
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = bundle.emoji, fontSize = 24.sp)
                        Text(
                            text = bundle.title,
                            style = MaterialTheme.typography.subtitle1,
                            color = MaterialTheme.colors.primaryVariant
                        )
                    }
                }

            }
        }
    }
}

@Composable
private fun InfoSection() {
    Column(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
    ) {
        LinkifyText(text = "API provided by ", link = "https://api.nasa.gov/")
        LinkifyText(text = "Email: ", link = "mailto:sasha.sirelon@gmail.com")
        Text(
            text = "Version: ${BuildConfig.VERSION_NAME}",
            modifier = Modifier.padding(4.dp)
        )
    }
}

@Composable
private fun ThemeChanger() {
    val currentTheme by Prefs.themeLiveData.observeAsState(initial = Prefs.theme)
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
            Title("Change theme of application")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RadioButtonText(text = "White", selected = currentTheme == Theme.WHITE) {
                    changeColor(Theme.WHITE)
                }
                RadioButtonText(text = "Dark", selected = currentTheme == Theme.DARK) {
                    changeColor(Theme.DARK)
                }

                RadioButtonText(text = "System", selected = currentTheme == Theme.SYSTEM) {
                    changeColor(Theme.SYSTEM)
                }
            }
        }
    }
}

@Composable
private fun Title(title: String) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = title,
        style = MaterialTheme.typography.h6.copy(
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.primaryVariant
        ),
    )
}

private fun changeColor(theme: Theme) {
    Prefs.theme = theme
    RoverApplication.APP.dataManger.trackEvent("change_theme_$theme")
}

@Composable
fun LinkifyText(text: String, link: String) {
    val uriHandler = LocalUriHandler.current

    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    val colors = MaterialTheme.colors

    val apiString = AnnotatedString.Builder(text).apply {
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
            layoutResult.value?.let {
                val position = it.getOffsetForPosition(offset)
                apiString.getStringAnnotations(position, position).firstOrNull()
                    ?.let { result ->
                        if (result.tag == "URL") {
                            uriHandler.openUri(result.item)
                        }
                    }
            }
        }
    }

    Text(
        text = apiString,
        modifier = Modifier
            .padding(4.dp)
            .then(tapGesture),
        onTextLayout = { layoutResult.value = it })
}