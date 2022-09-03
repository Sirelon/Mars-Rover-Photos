package com.sirelon.marsroverphotos.feature.rovers

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.Formatter
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import coil.ImageLoader
import coil.util.CoilUtils
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.*
import com.google.android.gms.common.util.CollectionUtils.listOf
import com.google.android.gms.common.util.CollectionUtils.mapOf
import com.google.common.io.Files.append
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.RoverApplication
import com.sirelon.marsroverphotos.extensions.recordException
import com.sirelon.marsroverphotos.feature.favorite.FavoriteScreen
import com.sirelon.marsroverphotos.feature.favorite.PopularScreen
import com.sirelon.marsroverphotos.feature.gdpr.GdprHelper
import com.sirelon.marsroverphotos.feature.images.ImageScreen
import com.sirelon.marsroverphotos.feature.photos.RoverPhotosScreen
import com.sirelon.marsroverphotos.feature.settings.AboutAppContent
import com.sirelon.marsroverphotos.models.Rover
import com.sirelon.marsroverphotos.models.drawableRes
import com.sirelon.marsroverphotos.storage.Prefs
import com.sirelon.marsroverphotos.storage.Theme
import com.sirelon.marsroverphotos.ui.MarsRoverPhotosTheme
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Collections.emptyList


@ExperimentalAnimationApi
@ExperimentalPagerApi
class RoversActivity : FragmentActivity() {

    private val gdprHelper = GdprHelper(this)

    // Determine the screen width (less decorations) to use for the ad width.
    // If the ad hasn't been laid out, default to the full screen width.
    private val adSize: AdSize
        get() {
            val display = windowManager.defaultDisplay
            val outMetrics = DisplayMetrics()
            display.getMetrics(outMetrics)

            val density = outMetrics.density

            val adWidthPixels = outMetrics.widthPixels.toFloat()

            val adWidth = (adWidthPixels / density).toInt()
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
        }

    private lateinit var adView: AdView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bottomItems = listOf(Screen.Rovers, Screen.Favorite, Screen.Popular, Screen.About)

        if (RoverApplication.APP.adEnabled) {
            gdprHelper.init()
        }

        setContent {
            val theme by Prefs.themeLiveData.observeAsState(initial = Prefs.theme)
            LaunchedEffect(key1 = theme) {
                track("theme_$theme")
            }

            val isDark: Boolean =
                if (theme == Theme.SYSTEM) isSystemInDarkTheme() else theme == Theme.DARK
            MarsRoverPhotosTheme(isDark) {
                val navController = rememberNavController()

                Scaffold(
                    topBar = {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // TODO:
                                }
                                .drawBehind {
                                    val halfHeight = size.height / 2
                                    drawRect(
                                        color = Color.Blue,
                                        size = size.copy(height = halfHeight)
                                    )
                                    drawRect(
                                        color = Color.Yellow,
                                        size = size.copy(height = halfHeight),
                                        topLeft = Offset(x = 0f, y = halfHeight)
                                    )
                                }
                                .shadow(elevation = 2.dp)
                        ) {
                            Text(
                                text = "#Stand with Ukraine",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                textAlign = TextAlign.Center,
                                color = Color.Red,
                                style = MaterialTheme.typography.h6.copy(
                                    shadow = Shadow(
                                        color = Color.Black,
                                        offset = Offset(2f, 2f),
                                        blurRadius = 2f
                                    )
                                )
                            )
                        }


                    },
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

        // Configurate ads
        MobileAds.initialize(this@RoversActivity) {
            Log.d("RoverApplication", "On Add Init status $it")
        }
//        val testDeviceIds =
//            listOf("235F224A866C9DFBEB26755C3E0337B3", AdRequest.DEVICE_ID_EMULATOR)
        val configuration =
            RequestConfiguration.Builder()
//                .setTestDeviceIds(testDeviceIds)
                .build()
        MobileAds.setRequestConfiguration(configuration)

        adView = AdView(this)
        adView.setAdSize(adSize)
        // Test
//        adView.adUnitId = "ca-app-pub-3940256099942544/6300978111"
        adView.adUnitId = "ca-app-pub-7516059448019339/9309101894"
//        adView.adUnitId = "ca-app-pub-7516059448019339/2257199658"
    }


    private fun clearCache() {
        track("Clear cache")
        val ctx = application
        lifecycleScope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, throwable -> throwable.printStackTrace() }) {
            val diskCache = ImageLoader(ctx).diskCache ?: return@launch

            val coilSize = diskCache.size
            diskCache.clear()

            val sizeStr = Formatter.formatFileSize(ctx, coilSize)
            RoverApplication.APP.dataManger.trackEvent("cache_cleared", mapOf("Size" to sizeStr))
            withContext(Dispatchers.Main) {
                Toast.makeText(ctx, "Cleared $sizeStr", Toast.LENGTH_SHORT).show()
            }
        }
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
            val currentRoute = navBackStackEntry?.destination?.route
            bottomItems.forEach { screen ->
                BottomNavigationItem(
                    icon = {
                        Icon(
                            screen.iconCreator.invoke(),
                            contentDescription = null
                        )
                    },
                    selected = currentRoute == screen.route,
                    selectedContentColor = MaterialTheme.colors.secondary,
                    unselectedContentColor = Color.White.copy(alpha = ContentAlpha.medium),
                    onClick = {
                        track("bottom_${screen.route}")

                        navController.navigate(screen.route) {
                            // Pop up to the start destination of the graph to
                            // avoid building up a large stack of destinations
                            // on the back stack as users select items
                            popUpTo(navController.graph.startDestinationId) {

                            }
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
        if (!RoverApplication.APP.adEnabled) {
            Box(modifier)
            return
        }

        val personalized by gdprHelper.acceptGdpr.collectAsState(initial = false)

        AndroidView<View>(modifier = modifier, factory = { adView }) {
            val adRequest = AdRequest
                .Builder()
//                .let {
//                    if (!personalized) {
//                        val extras = Bundle()
//                        extras.putString("npa", "1")
//
//                        it.addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
//                    } else it
//                }
                .build()
            Timber.d("ComposableBannerAd called $personalized");
            // Start loading the ad in the background.
            adView.loadAd(adRequest)

        }
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
                        track("click_rover_${it.name}")
                        navController.navigate("rover/${it.id}")
                    })
            }
            composable(Screen.About.route) {
                AboutAppContent(
                    onClearCache = ::clearCache,
                    onRateApp = ::goToMarket
                )
            }

            composable(Screen.Popular.route) {
                PopularScreen(navController)
            }

            composable(Screen.Favorite.route) {
                FavoriteScreen(navController)
            }

            composable(
                "rover/{roverId}",
                arguments = listOf(navArgument("roverId") {
                    type = NavType.LongType
                })
            ) {
                val roverId = it.arguments?.getLong("roverId")
                if (roverId != null) {
                    RoverPhotosScreen(
                        this@RoversActivity,
                        roverId = roverId,
                        navHost = navController
                    )
                }
            }


            composable(
                route = "photos/{pid}?ids={ids}",
                arguments = listOf(
                    navArgument("pid") { type = NavType.StringType },
                    navArgument("ids") { type = NavType.StringType })
            ) {
                val ids = it.arguments?.getString("ids")?.split(", ")?.toList()
                val selectedImage = it.arguments?.getString("pid")

                if (ids.isNullOrEmpty()) {
                    recordException(IllegalArgumentException("Try to open ${it.id} with $ids and $selectedImage"))
                } else {
                    ImageScreen(
                        activity = this@RoversActivity,
                        photoIds = ids,
                        selectedId = selectedImage
                    )
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

    class Images(val ids: List<String>) : Screen("photos", { Icons.Outlined.ViewCarousel })
}

@Composable
fun RoversContent(
    rovers: List<Rover>,
    onClick: (rover: Rover) -> Unit
) {
    LazyColumn {
        items(rovers) { item ->
            RoverItem(rover = item, onClick = onClick)
            Divider()
        }
    }
}

@Composable
fun RoverItem(rover: Rover, onClick: (rover: Rover) -> Unit) {
    Timber.d("RoverItem() called with: rover = $rover");
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
            val drawableRes = rover.drawableRes(LocalContext.current)
            Image(
                contentScale = ContentScale.FillHeight,
                painter = painterResource(id = drawableRes),
                modifier = height
                    .weight(1f)
                    .clip(shape = MaterialTheme.shapes.large),
                contentDescription = rover.name
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = height.weight(1f),
                verticalArrangement = Arrangement.SpaceAround
            ) {
                InfoText(
                    label = stringResource(id = com.sirelon.marsroverphotos.R.string.label_photos_total),
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
