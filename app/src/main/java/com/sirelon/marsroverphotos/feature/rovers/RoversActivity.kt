package com.sirelon.marsroverphotos.feature.rovers

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.util.Pair
import androidx.navigation.NavType
import androidx.navigation.compose.KEY_ROUTE
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navArgument
import androidx.navigation.compose.navigate
import androidx.navigation.compose.rememberNavController
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.activity.ComposeAboutAppActivity
import com.sirelon.marsroverphotos.activity.PhotosActivity
import com.sirelon.marsroverphotos.activity.RxActivity
import com.sirelon.marsroverphotos.activity.ui.MarsRoverPhotosTheme
import com.sirelon.marsroverphotos.activity.ui.accent
import com.sirelon.marsroverphotos.feature.favorite.FavoriteItem
import com.sirelon.marsroverphotos.feature.favorite.FavoritePhotosActivity
import com.sirelon.marsroverphotos.feature.favorite.FavoriteScreen
import com.sirelon.marsroverphotos.feature.favorite.PopularScreen
import com.sirelon.marsroverphotos.feature.photos.RoverPhotosScreen
import com.sirelon.marsroverphotos.feature.popular.PopularItem
import com.sirelon.marsroverphotos.feature.popular.PopularPhotosActivity
import com.sirelon.marsroverphotos.models.Rover
import com.sirelon.marsroverphotos.models.ViewType
import com.skydoves.landscapist.glide.GlideImage

@OptIn(ExperimentalFoundationApi::class)
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

        val bottomItems = listOf(Screen.Rovers, Screen.Favorite, Screen.Popular, Screen.About)

        setContent {
            MarsRoverPhotosTheme {
                val navController = rememberNavController()

                Scaffold(
                    bottomBar = {
                        BottomNavigation() {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentRoute = navBackStackEntry?.arguments?.getString(KEY_ROUTE)
                            bottomItems.forEach { screen ->
                                BottomNavigationItem(
                                    icon = { Icon(screen.icon, contentDescription = null) },
                                    selected = currentRoute == screen.route,
                                    selectedContentColor = accent,
                                    unselectedContentColor = Color.White.copy(alpha = ContentAlpha.medium),
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            // Pop up to the start destination of the graph to
                                            // avoid building up a large stack of destinations
                                            // on the back stack as users select items
                                            popUpTo = navController.graph.startDestination
                                            // Avoid multiple copies of the same destination when
                                            // reselecting the same item
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                        }
                    },
                    floatingActionButton = {
//                        FloatingActionButton(onClick = { }) {
//                            Icon(
//                                painter = painterResource(id = R.drawable.ic_autorenew),
//                                contentDescription = "refresh"
//                            )
//                        }
                    }
                ) { paddigValues ->
                    NavHost(navController = navController, startDestination = Screen.Rovers.route) {
                        val modifier =
                            Modifier.padding(bottom = paddigValues.calculateBottomPadding())
                        composable(Screen.Rovers.route) {
                            val rovers by dataManager.rovers.observeAsState(emptyList())

                            RoversContent(
                                modifier = modifier,
                                rovers = rovers,
                                onClick = {
                                    if (it is Rover) {
                                        navController.navigate("rover/${it.id}")
                                    }

//                                    onModelChoose(it)
                                })
                        }
                        composable(Screen.About.route) {
                            ComposeAboutAppActivity().AboutAppContent()
                        }

                        composable(Screen.Popular.route) {
                            PopularScreen(modifier, this@RoversActivity)
                        }

                        composable(Screen.Favorite.route) {
                            FavoriteScreen(modifier, this@RoversActivity)
                        }

                        composable(
                            "rover/{roverId}",
                            arguments = listOf(navArgument("roverId") { type = NavType.LongType })
                        ) {
                            val roverId = it.arguments?.getLong("roverId")
                            if (roverId != null) {
                                RoverPhotosScreen(this@RoversActivity, modifier, roverId)
                            }
                        }

                    }
                }
            }
        }
    }
}

sealed class Screen(val route: String, val icon: ImageVector) {
    object Favorite : Screen("favorite", Icons.Outlined.Favorite)
    object Popular : Screen("popular", Icons.Outlined.Explore)
    object About : Screen("about", Icons.Outlined.Info)
    object Rovers : Screen("rovers", Icons.Outlined.ViewCarousel)
    class Rover(val id: Long) : Screen("rover", Icons.Outlined.ViewCarousel)
}

@Composable
fun RoversContent(modifier: Modifier, rovers: List<Rover>, onClick: (rover: ViewType) -> Unit) {
    val popular = PopularItem()
    val favoriteItem = FavoriteItem(null)
    val items = rovers.toMutableList<ViewType>()
//    items.add(0, popular)
//    items.add(1, favoriteItem)
    LazyColumn(modifier = modifier) {
        items(items) { item ->
            when (item) {
                is Rover -> RoverItem(rover = item, onClick = onClick)
                is PopularItem -> PopularItem(item, onClick = onClick)
                is FavoriteItem -> FavoriteItem(item, onClick = onClick)
            }

            Divider()
        }
    }
}

@Composable
fun FavoriteItem(rover: FavoriteItem, onClick: (rover: FavoriteItem) -> Unit) {
    CommonItem(
        title = stringResource(id = R.string.favorite_title),
        imageAsset = painterResource(id = R.drawable.popular),
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
        imageAsset = painterResource(id = R.drawable.popular),
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

        val height = Modifier.height(175.dp)
        Row(modifier = Modifier.padding(8.dp)) {
            GlideImage(
                contentScale = ContentScale.FillHeight,
                imageModel = rover.iamgeUrl ?: "",
                modifier = height
                    .weight(1f)
                    .clip(shape = MaterialTheme.shapes.large)
            )
            Spacer(modifier = Modifier.width(8.dp))
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
fun CommonItem(title: String, imageAsset: Painter, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(16.dp)
            .clickable(onClick = onClick)
    ) {
        TitleText(title)
        Spacer(modifier = Modifier.height(8.dp))
        Image(
            painter = imageAsset,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .clip(MaterialTheme.shapes.large)
                .fillMaxWidth(),
            contentDescription = null
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
