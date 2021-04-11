package com.sirelon.marsroverphotos.feature.rovers

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.Formatter
import android.view.View
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalFireDepartment
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
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.KEY_ROUTE
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navArgument
import androidx.navigation.compose.navigate
import androidx.navigation.compose.rememberNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.cache.DiskCache
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.sirelon.marsroverphotos.BuildConfig
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.RoverApplication
import com.sirelon.marsroverphotos.activity.AboutAppContent
import com.sirelon.marsroverphotos.activity.ui.MarsRoverPhotosTheme
import com.sirelon.marsroverphotos.activity.ui.accent
import com.sirelon.marsroverphotos.extensions.logD
import com.sirelon.marsroverphotos.feature.favorite.FavoriteItem
import com.sirelon.marsroverphotos.feature.favorite.FavoriteScreen
import com.sirelon.marsroverphotos.feature.favorite.PopularScreen
import com.sirelon.marsroverphotos.feature.photos.RoverPhotosScreen
import com.sirelon.marsroverphotos.feature.popular.PopularItem
import com.sirelon.marsroverphotos.models.Rover
import com.sirelon.marsroverphotos.models.ViewType
import com.sirelon.marsroverphotos.utils.screenWidth
import com.skydoves.landscapist.glide.GlideImage
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class RoversActivity : AppCompatActivity() {

    // Determine the screen width (less decorations) to use for the ad width.
    // If the ad hasn't been laid out, default to the full screen width.
    private val adSize: AdSize
        get() {
            val density = resources.configuration.densityDpi

//            var adWidthPixels = ad_view_container.width.toFloat()
//            if (adWidthPixels == 0f) {
//                val adWidthPixels = outMetrics.widthPixels.toFloat()
//            }

            val adWidth = (screenWidth / density).toInt()
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
        }

    private lateinit var adView: AdView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adView = AdView(this)
        adView.adSize = AdSize.BANNER
//        adView.adUnitId = "ca-app-pub-3940256099942544/6300978111"
        adView.adUnitId = "ca-app-pub-7516059448019339/9309101894"

        val bottomItems = listOf(Screen.Rovers, Screen.Favorite, Screen.Popular, Screen.About)

        setContent {
            track("is_dark_${isSystemInDarkTheme()}")

            MarsRoverPhotosTheme {
                val navController = rememberNavController()
                Scaffold(
                    bottomBar = {
                        RoversBottomBar(navController, bottomItems)
                    },
                    content = { paddingValues ->
                        ConstraintLayout(
                            modifier = Modifier
                                .padding(paddingValues)
                                .fillMaxSize()
                        ) {
                            val (content, ad) = createRefs()

                            val contentModifier = Modifier.constrainAs(content) {
                                height = Dimension.fillToConstraints
                                bottom.linkTo(ad.top)
                                top.linkTo(parent.top)
                            }

                            Box(modifier = contentModifier) {
                                RoversNavHost(navController)
                            }
                            val adModifier = Modifier.constrainAs(ad) {
                                bottom.linkTo(parent.bottom)
                                end.linkTo(parent.end)
                                start.linkTo(parent.start)
                            }
                            ComposableBannerAd(adModifier)
                        }
                    })
            }
        }
    }


    private fun clearCache() {
        track("Clear cache")
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

    private fun goToMarket() {
        track("goToMarket")
        val uri = Uri.parse("market://details?id=${this.packageName}")
        val goToMarket = Intent(Intent.ACTION_VIEW, uri)
        try {
            startActivity(goToMarket)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=${this.packageName}")
            )
            if (intent.resolveActivity(this.packageManager) != null) {
                startActivity(intent)
            }
        }
    }

    @Composable
    private fun RoversBottomBar(
        navController: NavHostController,
        bottomItems: List<Screen>
    ) {
        BottomNavigation {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute =
                navBackStackEntry?.arguments?.getString(KEY_ROUTE)
            bottomItems.forEach { screen ->
                BottomNavigationItem(
                    icon = {
                        Icon(
                            screen.iconCreator.invoke(),
                            contentDescription = null
                        )
                    },
                    selected = currentRoute == screen.route,
                    selectedContentColor = accent,
                    unselectedContentColor = Color.White.copy(alpha = ContentAlpha.medium),
                    onClick = {
                        track("bottom_${screen.route}")

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
    }

    @Composable
    private fun ComposableBannerAd(modifier: Modifier) {
        if (BuildConfig.DEBUG) return

        AndroidView<View>(modifier = modifier, factory = {
            val adRequest = AdRequest
                .Builder()
                .addTestDevice("235F224A866C9DFBEB26755C3E0337B3")
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build()

            // Start loading the ad in the background.
            adView.loadAd(adRequest)

            adView
        })
    }

    @Composable
    private fun RoversNavHost(navController: NavHostController) {
        NavHost(
            navController = navController,
            startDestination = Screen.Rovers.route
        ) {

            composable(Screen.Rovers.route) {
                val rovers by RoverApplication.APP.dataManger.rovers.observeAsState(emptyList())

                RoversContent(
                    rovers = rovers,
                    onClick = {
                        if (it is Rover) {
                            track("click_rover_${it.name}")
                            navController.navigate("rover/${it.id}")
                        }
                    })
            }
            composable(Screen.About.route) {
                AboutAppContent(onClearCache = ::clearCache, onRateApp = ::goToMarket)
            }

            composable(Screen.Popular.route) {
                PopularScreen(this@RoversActivity)
            }

            composable(Screen.Favorite.route) {
                FavoriteScreen(this@RoversActivity, navController)
            }

            composable(
                "rover/{roverId}",
                arguments = listOf(navArgument("roverId") {
                    type = NavType.LongType
                })
            ) {
                val roverId = it.arguments?.getLong("roverId")
                if (roverId != null) {
                    RoverPhotosScreen(this@RoversActivity, roverId = roverId)
                }
            }

        }
    }

    private fun track(track: String) {
        RoverApplication.APP.dataManger.trackClick(track)
    }

    override fun onResume() {
        super.onResume()
        adView.resume()
    }

    override fun onDestroy() {
        adView.destroy()
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        adView.pause()
    }
}

sealed class Screen(val route: String, val iconCreator: @Composable () -> ImageVector) {
    object Rovers : Screen("rovers", {
        ImageVector.vectorResource(id = R.drawable.ic_rovers)
    })

    object Favorite : Screen("favorite", { Icons.Outlined.Favorite })
    object Popular : Screen("popular", { Icons.Outlined.LocalFireDepartment })
    object About : Screen("about", { Icons.Outlined.Info })

    class Rover(val id: Long) : Screen("rover", { Icons.Outlined.ViewCarousel })
}

@Composable
fun RoversContent(
    modifier: Modifier = Modifier,
    rovers: List<Rover>,
    onClick: (rover: ViewType) -> Unit
) {
    "RoveerCoteent".logD()
    val items = rovers.toMutableList<ViewType>()
//    items.add(0, popular)
//    items.add(1, favoriteItem)
    LazyColumn {
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
