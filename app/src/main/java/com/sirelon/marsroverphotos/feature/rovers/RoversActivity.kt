package com.sirelon.marsroverphotos.feature.rovers

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.Formatter
import android.util.DisplayMetrics
import android.view.View
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import coil3.ImageLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.RoverApplication
import com.sirelon.marsroverphotos.feature.favorite.FavoriteScreen
import com.sirelon.marsroverphotos.feature.favorite.PopularScreen
import com.sirelon.marsroverphotos.feature.gdpr.GdprHelper
import com.sirelon.marsroverphotos.feature.imageIds
import com.sirelon.marsroverphotos.feature.images.ImageScreen
import com.sirelon.marsroverphotos.feature.photos.RoverPhotosScreen
import com.sirelon.marsroverphotos.feature.settings.AboutAppContent
import com.sirelon.marsroverphotos.feature.ukraine.UkraineBanner
import com.sirelon.marsroverphotos.feature.ukraine.UkraineInfoScreen
import com.sirelon.marsroverphotos.models.Rover
import com.sirelon.marsroverphotos.models.drawableRes
import com.sirelon.marsroverphotos.storage.Prefs
import com.sirelon.marsroverphotos.storage.Theme
import com.sirelon.marsroverphotos.ui.MarsRoverPhotosTheme
import com.sirelon.marsroverphotos.ui.MaterialSymbol
import com.sirelon.marsroverphotos.ui.MaterialSymbolIcon
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@ExperimentalAnimationApi
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class RoversActivity : FragmentActivity() {

    private val gdprHelper = GdprHelper(this)

    // Determine the screen width (less decorations) to use for the ad width.
    // If the ad hasn't been laid out, default to the full screen width.
    @Suppress("DEPRECATION")
    private val adSize: AdSize
        @SuppressLint("VisibleForTests")
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
        // https://stackoverflow.com/a/77262627/3127851
        window.decorView

        super.onCreate(savedInstanceState)

        val bottomItems = listOf(
            RoversDestination.Rovers,
            RoversDestination.Favorite,
            RoversDestination.Popular,
            RoversDestination.About,
        )

        if (RoverApplication.APP.adEnabled) {
            gdprHelper.init()
        }

        setContent {
            val theme by Prefs.themeLiveData.collectAsStateWithLifecycle()
            LaunchedEffect(key1 = theme) {
                track("theme_$theme")
            }

            val navState = remember { RoversNavigationState(RoversDestination.Rovers) }

            val viewModelStoreDecorator =
                rememberViewModelStoreNavEntryDecorator<RoversDestination>()
            val entryDecorators: List<NavEntryDecorator<RoversDestination>> =
                remember(viewModelStoreDecorator) {
                    listOf(viewModelStoreDecorator)
                }

            val activity = this@RoversActivity

            val isDark: Boolean =
                if (theme == Theme.SYSTEM) isSystemInDarkTheme() else theme == Theme.DARK

            var hideUI by remember {
                mutableStateOf(false)
            }

            BackHandler(enabled = navState.canGoBack()) {
                if (!navState.pop()) {
                    activity.finish()
                }
            }

            DisposableEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { isDark },
                    navigationBarStyle = SystemBarStyle.auto(
                        lightScrim,
                        darkScrim,
                    ) { isDark },
                )

                onDispose {}
            }

            MarsRoverPhotosTheme(isDark) {
                val windowSizeClass = calculateWindowSizeClass(activity)
                val navSuiteType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(
                    currentWindowAdaptiveInfo()
                )

                NavigationSuiteScaffold(
                    layoutType = navSuiteType,
                    navigationSuiteItems = {
                        bottomItems.forEach { destination ->
                            item(
                                icon = {
                                    when (destination) {
                                        RoversDestination.Rovers -> Icon(
                                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_rovers),
                                            contentDescription = null
                                        )

                                        RoversDestination.Favorite -> MaterialSymbolIcon(
                                            symbol = MaterialSymbol.Favorite,
                                            contentDescription = null
                                        )

                                        RoversDestination.Popular -> MaterialSymbolIcon(
                                            symbol = MaterialSymbol.LocalFireDepartment,
                                            contentDescription = null
                                        )

                                        RoversDestination.About -> MaterialSymbolIcon(
                                            symbol = MaterialSymbol.Info,
                                            contentDescription = null
                                        )
                                    }
                                },
                                label = {
                                    when (destination) {
                                        RoversDestination.Rovers -> Text(stringResource(R.string.nav_rovers))
                                        RoversDestination.Favorite -> Text(stringResource(R.string.nav_favorite))
                                        RoversDestination.Popular -> Text(stringResource(R.string.nav_popular))
                                        RoversDestination.About -> Text(stringResource(R.string.nav_about))
                                    }
                                },
                                selected = destination == navState.currentTopLevel,
                                onClick = {
                                    track("bottom_${destination.analyticsTag}")
                                    navState.selectTopLevel(destination, resetToTop = true)
                                }
                            )
                        }
                    },
                    content = {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Ukraine banner
                            AnimatedVisibility(
                                visible = !hideUI,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically(),
                            ) {
                                UkraineBanner(modifier = Modifier.statusBarsPadding()) {
                                    RoverApplication.APP.tracker.trackClick("UkraineBanner_Top")
                                    navState.push(RoversDestination.Ukraine, singleTop = true)
                                }
                            }

                            // Main content
                            Box(modifier = Modifier.weight(1f)) {
                                MarsRoverContent(
                                    modifier = Modifier.fillMaxSize(),
                                    activity = activity,
                                    navState = navState,
                                    entryDecorators = entryDecorators,
                                    onExit = { activity.finish() },
                                    onHideUi = { hideUI = it }
                                )
                            }
                        }
                    },
                )
            }
        }

        // Configurate ads
        MobileAds.initialize(this@RoversActivity) {
            Timber.d("On Add Init status " + it)
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
    private fun MarsRoverContent(
        modifier: Modifier,
        activity: FragmentActivity,
        navState: RoversNavigationState,
        entryDecorators: List<NavEntryDecorator<RoversDestination>>,
        onExit: () -> Unit,
        onHideUi: (Boolean) -> Unit,
    ) {
        Column(
            modifier = modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.weight(1f)) {
                RoversNavDisplay(
                    activity = activity,
                    navState = navState,
                    entryDecorators = entryDecorators,
                    onExit = onExit,
                    onHideUi = onHideUi,
                )
            }
            ComposableBannerAd(modifier = Modifier.fillMaxWidth())
        }
    }

//    @Composable
//    private fun RoversBottomBar(
//        bottomItems: List<RoversDestination.TopLevel>,
//        currentDestination: RoversDestination.TopLevel,
//        onSelect: (RoversDestination.TopLevel) -> Unit,
//    ) {
//        NavigationBar(modifier = Modifier.navigationBarsPadding()) {
//            bottomItems.forEach { destination ->
//                NavigationBarItem(
//                    icon = {
//                        when (destination) {
//                            RoversDestination.Rovers -> Icon(
//                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_rovers),
//                                contentDescription = null
//                            )
//
//                            RoversDestination.Favorite -> MaterialSymbolIcon(
//                                symbol = MaterialSymbol.Favorite,
//                                contentDescription = null
//                            )
//
//
//                            RoversDestination.Popular -> MaterialSymbolIcon(
//                                symbol = MaterialSymbol.ViewList,
//                                contentDescription = null
//                            )
//
//                            RoversDestination.About -> MaterialSymbolIcon(
//                                symbol = MaterialSymbol.Info,
//                                contentDescription = null
//                            )
//                        }
//                    },
//                    selected = destination == currentDestination,
//                    onClick = { onSelect(destination) }
//                )
//            }
//        }
//    }

    @Composable
    private fun ComposableBannerAd(modifier: Modifier) {
        if (!RoverApplication.APP.adEnabled) {
            Box(modifier)
            return
        }

        val personalized by gdprHelper.acceptGdpr.collectAsStateWithLifecycle(initialValue = false)

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
            Timber.d("ComposableBannerAd called $personalized")
            // Start loading the ad in the background.
            adView.loadAd(adRequest)
        }
    }

    @Composable
    private fun RoversNavDisplay(
        activity: FragmentActivity,
        navState: RoversNavigationState,
        entryDecorators: List<NavEntryDecorator<RoversDestination>>,
        onExit: () -> Unit,
        onHideUi: (Boolean) -> Unit,
    ) {
        NavDisplay(
            backStack = navState.backStack,
            onBack = {
                if (!navState.pop()) {
                    onExit()
                }
            },
            entryDecorators = entryDecorators,
            entryProvider = entryProvider {
                entry<RoversDestination.Rovers> {
                    val rovers by RoverApplication.APP.dataManger.rovers.collectAsStateWithLifecycle(
                        initialValue = emptyList()
                    )

                    RoversContent(
                        rovers = rovers,
                        onClick = {
                            track("click_rover_${it.name}")
                            navState.push(RoversDestination.RoverDetail(it.id))
                        }
                    )
                }
                entry<RoversDestination.About> {
                    AboutAppContent(
                        onClearCache = ::clearCache,
                        onRateApp = ::goToMarket
                    )
                }
                entry<RoversDestination.Popular> {
                    PopularScreen(
                        onNavigateToImages = { image, photos ->
                            navState.push(
                                RoversDestination.ImageGallery(
                                    ids = photos.imageIds(),
                                    selectedId = image.id,
                                    shouldTrack = false
                                )
                            )
                        }
                    )
                }
                entry<RoversDestination.Favorite> {
                    FavoriteScreen(
                        onNavigateToImages = { image, photos, tracking ->
                            navState.push(
                                RoversDestination.ImageGallery(
                                    ids = photos.imageIds(),
                                    selectedId = image.id,
                                    shouldTrack = tracking
                                )
                            )
                        },
                        onNavigateToRovers = {
                            navState.selectTopLevel(RoversDestination.Rovers, resetToTop = true)
                        }
                    )
                }
                entry<RoversDestination.Ukraine> {
                    UkraineInfoScreen()
                }
                entry<RoversDestination.RoverDetail> { destination ->
                    RoverPhotosScreen(
                        activity = activity,
                        roverId = destination.roverId,
                        onNavigateToImages = { image, photos ->
                            navState.push(
                                RoversDestination.ImageGallery(
                                    ids = photos.imageIds(),
                                    selectedId = image.id,
                                    shouldTrack = true
                                )
                            )
                        }
                    )
                }
                entry<RoversDestination.ImageGallery> { gallery ->
                    ImageScreen(
                        trackingEnabled = gallery.shouldTrack,
                        photoIds = gallery.ids,
                        selectedId = gallery.selectedId,
                        onHideUi = onHideUi,
                    )
                }
            }
        )
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

private sealed interface RoversDestination {
    sealed interface TopLevel : RoversDestination {
        val analyticsTag: String
    }

    data object Rovers : TopLevel {
        override val analyticsTag: String = "rovers"
    }

    data object Favorite : TopLevel {
        override val analyticsTag: String = "favorite"
    }

    data object Popular : TopLevel {
        override val analyticsTag: String = "popular"
    }

    data object About : TopLevel {
        override val analyticsTag: String = "about"
    }

    data object Ukraine : RoversDestination
    data class RoverDetail(val roverId: Long) : RoversDestination
    data class ImageGallery(
        val ids: List<String>,
        val selectedId: String?,
        val shouldTrack: Boolean,
    ) : RoversDestination
}

private class RoversNavigationState(start: RoversDestination.TopLevel) {

    private val stacks: LinkedHashMap<RoversDestination.TopLevel, SnapshotStateList<RoversDestination>> =
        linkedMapOf(start to mutableStateListOf<RoversDestination>().apply { add(start) })

    var currentTopLevel by mutableStateOf(start)
        private set

    val backStack: SnapshotStateList<RoversDestination> =
        mutableStateListOf<RoversDestination>().apply {
            add(start)
        }

    private fun createStack(destination: RoversDestination.TopLevel) =
        mutableStateListOf<RoversDestination>().apply { add(destination) }

    private fun rebuildBackStack() {
        backStack.clear()
        stacks.values.forEach { stack ->
            backStack.addAll(stack)
        }
    }

    fun selectTopLevel(destination: RoversDestination.TopLevel, resetToTop: Boolean = false) {
        val stack = stacks.remove(destination) ?: createStack(destination)
        if (resetToTop) {
            stack.clear()
            stack.add(destination)
        } else if (stack.isEmpty()) {
            stack.add(destination)
        }
        stacks[destination] = stack
        currentTopLevel = destination
        rebuildBackStack()
    }

    fun push(destination: RoversDestination, singleTop: Boolean = false) {
        if (destination is RoversDestination.TopLevel) {
            selectTopLevel(destination, resetToTop = true)
            return
        }

        val stack = stacks[currentTopLevel] ?: return
        if (singleTop) {
            removeFromStacks(destination)
        } else {
            stack.remove(destination)
        }
        stack.add(destination)
        rebuildBackStack()
    }

    private fun removeFromStacks(destination: RoversDestination) {
        stacks.values.forEach { stack ->
            stack.remove(destination)
        }
    }

    fun pop(): Boolean {
        val stack = stacks[currentTopLevel] ?: return false
        if (stack.size > 1) {
            stack.removeAt(stack.lastIndex)
            rebuildBackStack()
            return true
        }

        if (stacks.size == 1) {
            return false
        }

        stacks.remove(currentTopLevel)
        currentTopLevel = stacks.keys.last()
        rebuildBackStack()
        return true
    }

    fun canGoBack(): Boolean = backStack.size > 1
}

@Composable
fun RoversContent(
    rovers: List<Rover>,
    onClick: (rover: Rover) -> Unit
) {
    LazyColumn {
        items(rovers) { item ->
            RoverItem(rover = item, onClick = onClick)
            HorizontalDivider()
        }
    }
}

@Composable
fun RoverItem(rover: Rover, onClick: (rover: Rover) -> Unit) {
    Timber.d("RoverItem() called with: rover = $rover")
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
        text = text,
        style = typography.headlineMedium,
        color = MaterialTheme.colorScheme.tertiary
    )
}

@Composable
private fun InfoText(label: String, text: String) {
    val typography = MaterialTheme.typography
    val textToShow = AnnotatedString.Builder().apply {
        pushStyle(
            typography.titleMedium.toSpanStyle()
                .copy(color = MaterialTheme.colorScheme.secondary)
        )
        append("$label ")
        pushStyle(
            style = typography.titleSmall.toSpanStyle()
                .copy(color = MaterialTheme.colorScheme.onSurface)
        )
        append(text)
    }
    Text(
        text = textToShow.toAnnotatedString(),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * The default light scrim, as defined by androidx and the platform:
 * https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:activity/activity/src/main/java/androidx/activity/EdgeToEdge.kt;l=35-38;drc=27e7d52e8604a080133e8b842db10c89b4482598
 */
private val lightScrim = android.graphics.Color.argb(0xe6, 0xFF, 0xFF, 0xFF)

/**
 * The default dark scrim, as defined by androidx and the platform:
 * https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:activity/activity/src/main/java/androidx/activity/EdgeToEdge.kt;l=40-44;drc=27e7d52e8604a080133e8b842db10c89b4482598
 */
private val darkScrim = android.graphics.Color.argb(0x80, 0x1b, 0x1b, 0x1b)
