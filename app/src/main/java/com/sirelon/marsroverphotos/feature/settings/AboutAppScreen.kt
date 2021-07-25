package com.sirelon.marsroverphotos.activity

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
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
import com.sirelon.marsroverphotos.activity.ui.MarsRoverPhotosTheme
import java.util.Calendar

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MarsRoverPhotosTheme {
        AboutAppContent({}, {})
    }
}

@Composable
fun AboutAppContent(onClearCache: () -> Unit, onRateApp: () -> Unit) {
    MaterialTheme {

        val typography = MaterialTheme.typography
        val colors = MaterialTheme.colors
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .then(Modifier.padding(16.dp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Image(
                    painter = painterResource(id = R.drawable.alien_icon),
                    contentDescription = null
                )
                Spacer(Modifier.width(16.dp))
                Text(text = "Mars rover photos", style = typography.h5)
                Text(
                    text = stringResource(id = R.string.about_description),
                    style = typography.body1,
                    textAlign = TextAlign.Center,
                    color = colors.secondaryVariant
                )
                // TODO
                Spacer(modifier = Modifier.fillMaxSize())
                TextButton(onClick = onClearCache) {
                    Text(text = stringResource(id = R.string.clear_cache))
                }

                Column(
                    modifier = Modifier
                        .padding(vertical = 24.dp)
                        .fillMaxWidth()
                ) {
                    LinkifyText(text = "API provided by ", link = "https://api.nasa.gov/")
                    LinkifyText(text = "Email: ", link = "mailto:sasha.sirelon@gmail.com")
                    Text(
                        text = "Version: ${BuildConfig.VERSION_NAME}",
                        modifier = Modifier.padding(4.dp)
                    )
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
    }
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