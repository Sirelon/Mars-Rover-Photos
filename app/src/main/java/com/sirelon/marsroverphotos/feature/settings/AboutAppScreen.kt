package com.sirelon.marsroverphotos.feature.settings

import androidx.compose.foundation.Image
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
        AboutAppContent({}, {})
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

        ThemeChanger()

        OutlinedButton(onClick = onClearCache) {
            Text(text = stringResource(id = R.string.clear_cache))
        }

        Button(onClick = onRateApp) {
            Text(text = stringResource(id = R.string.action_rate))
        }
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
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Change theme of application",
                style = MaterialTheme.typography.h6.copy(textAlign = TextAlign.Center)
            )
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