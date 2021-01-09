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
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.util.Pair
import androidx.ui.tooling.preview.Preview
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
//                Surface(color = MaterialTheme.colors.background) {
//                    RoversContent(dataManager) {
//                        onModelChoose(it)
//                    }
//                }

                Scaffold(
                    bodyContent = {
                        val rovers by dataManager.rovers.observeAsState(emptyList())
                        RoversContent(
                            rovers = rovers,
                            onClick = { onModelChoose(it) })
                    },
                    bottomBar = {
                        BottomNavigation() {
                            BottomNavigationItem(
                                icon = { Icon(Icons.Outlined.Favorite) }, selected = true,
                                onClick = {})
                            BottomNavigationItem(
                                icon = { Icon(Icons.Outlined.ViewCarousel) }, selected = false,
                                onClick = {}
                            )
                            BottomNavigationItem(
                                icon = { Icon(Icons.Outlined.Info) }, selected = false,
                                onClick = {})
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun RoversContent(rovers: List<Rover>, onClick: (rover: ViewType) -> Unit) {
    MaterialTheme {
        val popular = PopularItem()
        val favoriteItem = FavoriteItem(null)
        val items = rovers.toMutableList<ViewType>()
        items.add(0, popular)
        items.add(1, favoriteItem)
        LazyColumnFor(items = items) { item ->
            when (item) {
                is Rover -> RoverItem(rover = item, onClick = onClick)
                is PopularItem -> PopularItem(item, onClick = onClick)
                is FavoriteItem -> FavoriteItem(item, onClick = onClick)
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
fun FavoriteItem(rover: FavoriteItem, onClick: (rover: FavoriteItem) -> Unit) {
    CommonItem(
        title = stringResource(id = R.string.favorite_title),
        imageAsset = imageResource(id = R.drawable.popular),
        onClick = { onClick(rover) })
    // Not implemented yet
//        InfoText(
//            label = stringResource(id = R.string.label_photos_total),
//            text = "${rover.totalPhotos}"
//        )
}

@Composable
fun PopularItem(rover: PopularItem, onClick: (rover: PopularItem) -> Unit) {
    CommonItem(
        title = stringResource(id = R.string.popular_title),
        imageAsset = imageResource(id = R.drawable.popular),
        onClick = { onClick(rover) })
    // Not implemented yet
//        InfoText(
//            label = stringResource(id = R.string.label_photos_total),
//            text = "${rover.totalPhotos}"
//        )
}

@Composable
fun RoverItem(rover: Rover, onClick: (rover: Rover) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(8.dp)
            .clickable(onClick = { onClick(rover) })
    ) {
        TitleText(rover.name)
        InfoText(label = "Status:", text = rover.status)

        val height = Modifier.preferredHeight(175.dp)
        Row(modifier = Modifier.padding(8.dp)) {
            GlideImage(
                contentScale = ContentScale.FillHeight,
                imageModel = rover.iamgeUrl ?: "",
                modifier = height
                    .weight(1f)
                    .clip(shape = MaterialTheme.shapes.small)
            )
            Spacer(modifier = Modifier.preferredWidth(8.dp))
            Column(
                modifier = height.weight(1f),
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
fun CommonItem(title: String, imageAsset: ImageBitmap, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(16.dp)
            .clickable(onClick = onClick)
    ) {
        TitleText(title)
        Spacer(modifier = Modifier.preferredHeight(8.dp))
        Image(
            bitmap = imageAsset,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .fillMaxWidth()
        )
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
