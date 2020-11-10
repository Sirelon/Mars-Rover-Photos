package com.sirelon.marsroverphotos.activity

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.Formatter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.gesture.tapGestureFilter
import androidx.compose.ui.platform.UriHandlerAmbient
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.annotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.ui.tooling.preview.Preview
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.cache.DiskCache
import com.sirelon.marsroverphotos.BuildConfig
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.RoverApplication
import com.sirelon.marsroverphotos.activity.ui.MarsRoverPhotosTheme
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

class ComposeAboutAppActivity : AppCompatActivity() {
    private val tracker = RoverApplication.APP.tracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MarsRoverPhotosTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    AboutAppContent()
                }
            }
        }
    }

    private fun clearCache() {
        tracker.trackClick("Clear cache")
        val ctx = application
        lifecycleScope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, throwable -> throwable.printStackTrace() }) {
            val cacheFile = File(cacheDir, DiskCache.Factory.DEFAULT_DISK_CACHE_DIR)
            val size = calculateSize(cacheFile)

            Glide.get(ctx).clearDiskCache()

            val sizeStr = Formatter.formatFileSize(ctx, size)
            withContext(Dispatchers.Main) {
                Toast.makeText(ctx, "Cleared $sizeStr", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun calculateSize(dir: File?): Long {
        if (dir == null) return 0
        if (!dir.isDirectory) return dir.length()
        var result: Long = 0
        val children: Array<File>? = dir.listFiles()
        if (children != null) for (child in children) result += calculateSize(child)
        return result
    }

    fun onRateApp() {
        goToMarket()
    }

    private fun goToMarket() {
        tracker.trackClick("goToMarket")
        val uri = Uri.parse("market://details?id=$packageName")
        val goToMarket = Intent(Intent.ACTION_VIEW, uri)
        try {
            startActivity(goToMarket)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            )
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            }
        }
    }


    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        MarsRoverPhotosTheme {
            AboutAppContent()
        }
    }

    @Composable
    fun AboutAppContent() {
        MaterialTheme {

            val typography = MaterialTheme.typography
            val colors = MaterialTheme.colors
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(asset = imageResource(id = R.drawable.alien_icon))
                Spacer(Modifier.preferredHeight(16.dp))
                Text(text = "Mars rover photos", style = typography.h5)
                Text(
                    text = stringResource(id = R.string.about_description),
                    style = typography.body1,
                    textAlign = TextAlign.Center,
                    color = colors.secondaryVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = ::clearCache) {
                    Text(text = stringResource(id = R.string.clear_cache))
                }

                Column(modifier = Modifier.padding(vertical = 24.dp).fillMaxWidth()) {
                    LinkifyText(text = "API provided by ", link = "https://api.nasa.gov/")
                    LinkifyText(text = "Email: ", link = "mailto:sasha.sirelon@gmail.com")
                    Text(
                        text = "Version: ${BuildConfig.VERSION_NAME}",
                        modifier = Modifier.padding(4.dp)
                    )
                }

                Button(onClick = ::onRateApp) {
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

    @Composable
    fun LinkifyText(text: String, link: String) {
        val uriHandler = UriHandlerAmbient.current

        val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

        val colors = MaterialTheme.colors

        val apiString = annotatedString {
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
        }

        val tapGesture = Modifier.tapGestureFilter { offset ->
            layoutResult.value?.let {
                val position = it.getOffsetForPosition(offset)
                apiString.getStringAnnotations(position, position).firstOrNull()?.let { result ->
                    if (result.tag == "URL") {
                        uriHandler.openUri(result.item)
                    }
                }
            }
        }

        Text(
            text = apiString,
            modifier = Modifier.padding(4.dp).then(tapGesture),
            onTextLayout = { layoutResult.value = it })
    }
}