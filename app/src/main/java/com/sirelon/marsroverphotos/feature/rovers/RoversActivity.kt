package com.sirelon.marsroverphotos.feature.rovers

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.preferredWidth
import androidx.compose.foundation.lazy.LazyColumnFor
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.util.Pair
import androidx.ui.tooling.preview.Preview
import com.sirelon.marsroverphotos.DataManager
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.activity.PhotosActivity
import com.sirelon.marsroverphotos.activity.RxActivity
import com.sirelon.marsroverphotos.activity.ui.MarsRoverPhotosTheme
import com.sirelon.marsroverphotos.feature.favorite.FavoriteItem
import com.sirelon.marsroverphotos.feature.favorite.FavoritePhotosActivity
import com.sirelon.marsroverphotos.feature.popular.PopularItem
import com.sirelon.marsroverphotos.feature.popular.PopularPhotosActivity
import com.sirelon.marsroverphotos.models.Rover
import com.sirelon.marsroverphotos.models.ViewType
import com.skydoves.landscapist.glide.GlideImage

class RoversActivity : RxActivity() {

    private fun onModelChoose(model: ViewType, vararg sharedElements: Pair<View, String>) {
        when (model) {
            is Rover -> startActivity(PhotosActivity.createIntent(this, model))
            is PopularItem -> startActivity(Intent(this, PopularPhotosActivity::class.java))
            is FavoriteItem -> startActivity(Intent(this, FavoritePhotosActivity::class.java))
//            is PopularItem -> FirebaseProvider.proideTestFirebase.deleteUnusedItems()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MarsRoverPhotosTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    RoversContent(dataManager) {
                        onModelChoose(it)
                    }
                }
            }
        }
    }
}

@Composable
fun RoversContent(dataManager: DataManager, onClick: (rover: ViewType) -> Unit) {
    MaterialTheme {
        val popular = PopularItem()
        val rovers: List<Rover> by dataManager.rovers.observeAsState(emptyList())
        val items = rovers.toMutableList<ViewType>()
        items.add(0, popular)
        LazyColumnFor(items = items) { item ->
            when (item) {
                is Rover -> RoverItem(rover = item, onClick = onClick)
                is PopularItem -> PopularItem(item, onClick = onClick)
            }

            Divider()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MarsRoverPhotosTheme {
        PopularItem(PopularItem()) {

        }
    }
}

@Composable
fun PopularItem(rover: PopularItem, onClick: (rover: PopularItem) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(16.dp)
            .clickable(onClick = { onClick(rover) })
    ) {
        TitleText(stringResource(id = R.string.popular_title))
        Spacer(modifier = Modifier.preferredHeight(8.dp))
        Image(
            asset = imageResource(id = R.drawable.popular),
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.clip(MaterialTheme.shapes.small).fillMaxWidth()
        )
        // Not implemented yet
//        InfoText(
//            label = stringResource(id = R.string.label_photos_total),
//            text = "${rover.totalPhotos}"
//        )
    }
}

@Composable
fun RoverItem(rover: Rover, onClick: (rover: Rover) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp).clickable(onClick = { onClick(rover) })
    ) {
        TitleText(rover.name)
        InfoText(label = "Status:", text = rover.status)

        val height = Modifier.preferredHeight(175.dp)
        Row(modifier = Modifier.padding(8.dp)) {
            GlideImage(
                contentScale = ContentScale.FillHeight,
                imageModel = rover.iamgeUrl ?: "",
                modifier = height + Modifier.weight(1f).clip(shape = MaterialTheme.shapes.small)
            )
            Spacer(modifier = Modifier.preferredWidth(8.dp))
            Column(
                modifier = height + Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceAround
            ) {
                InfoText(
                    label = stringResource(id = R.string.label_photos_total),
                    text = "${rover.totalPhotos}"
                )
                InfoText(label = "Last photo date:", text = rover.maxDate)
                InfoText(label = "Launch date from Earth:", text = rover.launchDate)
                InfoText(label = "Landing date on Mars:", text = rover.landingDate)
            }
        }
    }
}

@Composable
private fun TitleText(text: String) {
    val typography = MaterialTheme.typography
    Text(
        text = text, style = typography.h6, color = MaterialTheme.colors.secondary
    )
}

@Composable
fun InfoText(label: String, text: String) {
    val typography = MaterialTheme.typography
    val textToShow = AnnotatedString.Builder().apply {
        pushStyle(typography.subtitle2.toSpanStyle())
        append("$label ")
        pushStyle(style = typography.subtitle1.toSpanStyle())
        append(text)
    }
    Text(
        text = textToShow.toAnnotatedString(),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}
