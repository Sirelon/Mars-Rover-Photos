package com.sirelon.marsroverphotos.feature.rovers

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.Formatter
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import coil3.ImageLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.RoverApplication
import com.sirelon.marsroverphotos.feature.gdpr.GdprHelper
import com.sirelon.marsroverphotos.feature.ukraine.UkraineBanner
import com.sirelon.marsroverphotos.models.Rover
import com.sirelon.marsroverphotos.models.drawableRes
import com.sirelon.marsroverphotos.storage.Prefs
import com.sirelon.marsroverphotos.storage.Theme
import com.sirelon.marsroverphotos.ui.MarsRoverPhotosTheme
import com.sirelon.marsroverphotos.ui.MaterialSymbol
import com.sirelon.marsroverphotos.ui.MaterialSymbolIcon
import com.sirelon.marsroverphotos.widget.WidgetExtraImageId
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.navigation3.koinEntryProvider
import org.koin.core.annotation.KoinExperimentalAPI
import timber.log.Timber
import androidx.navigationevent.NavigationEventInfo
import com.sirelon.marsroverphotos.firebase.facts.EducationalFactsUploadDebugPanel
import com.sirelon.marsroverphotos.firebase.mission.MissionDataUploader

/**
 * Info class for debugging navigation state during predictive back.
 */
internal data class RoversNavigationInfo(
    val destination: RoversDestination
) : NavigationEventInfo() {
    override fun toString(): String = "RoversNavigationInfo(${destination::class.simpleName})"
}

@ExperimentalAnimationApi
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class RoversActivity : FragmentActivity() {

    private companion object {
        private const val WIDGET_IMAGE_ID_STATE_KEY = "widget_image_id_state"
    }

    private val gdprHelper = GdprHelper(this)
    private val widgetImageIdState = mutableStateOf<String?>(null)

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
        widgetImageIdState.value = savedInstanceState?.getString(WIDGET_IMAGE_ID_STATE_KEY)
            ?: intent?.getStringExtra(WidgetExtraImageId)

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

            val widgetImageId = widgetImageIdState.value
            LaunchedEffect(widgetImageId) {
                if (!widgetImageId.isNullOrBlank()) {
                    navState.push(
                        RoversDestination.ImageGallery(
                            ids = listOf(widgetImageId),
                            selectedId = widgetImageId,
                            shouldTrack = false
                        ),
                        singleTop = true
                    )
                    widgetImageIdState.value = null
                }
            }

            val savedStateDecorator =
                rememberSaveableStateHolderNavEntryDecorator<RoversDestination>()
            val viewModelStoreDecorator =
                rememberViewModelStoreNavEntryDecorator<RoversDestination>()
            val entryDecorators: List<NavEntryDecorator<RoversDestination>> =
                remember(savedStateDecorator, viewModelStoreDecorator) {
                    listOf(savedStateDecorator, viewModelStoreDecorator)
                }

            val activity = this@RoversActivity

            val isDark: Boolean =
                if (theme == Theme.SYSTEM) isSystemInDarkTheme() else theme == Theme.DARK

            var hideUI by remember {
                mutableStateOf(false)
            }
            val navTransition = updateTransition(targetState = hideUI, label = "navSuiteVisibility")
            val navAlpha by navTransition.animateFloat(label = "navAlpha") { hidden ->
                if (hidden) 0f else 1f
            }
            val navScale by navTransition.animateFloat(label = "navScale") { hidden ->
                if (hidden) 0.92f else 1f
            }

            val navigationEventState = rememberNavigationEventState(
                currentInfo = RoversNavigationInfo(navState.backStack.last()),
                backInfo = if (navState.backStack.size > 1) {
                    listOf(RoversNavigationInfo(navState.backStack[navState.backStack.lastIndex - 1]))
                } else {
                    emptyList()
                }
            )

            NavigationBackHandler(
                state = navigationEventState,
                isBackEnabled = navState.canGoBack(),
                onBackCancelled = {
                    // User cancelled the back gesture - no action needed
                },
                onBackCompleted = {
                    if (!navState.pop()) {
                        activity.finish()
                    }
                }
            )

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

                val layoutType =
                    if (hideUI && navAlpha <= 0.01f) NavigationSuiteType.None else navSuiteType

                NavigationSuiteScaffold(
                    layoutType = layoutType,
                    navigationSuiteItems = {
                        bottomItems.forEach { destination ->
                            item(
                                modifier = Modifier.graphicsLayer {
                                    alpha = navAlpha
                                    scaleX = navScale
                                    scaleY = navScale
                                },
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
                                enabled = !hideUI,
                                onClick = {
                                    track("bottom_${destination.analyticsTag}")
                                    navState.selectTopLevel(destination, resetToTop = true)
                                }
                            )
                        }
                    }
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Ukraine banner
                        AnimatedVisibility(
                            visible = !hideUI,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                        ) {
                            UkraineBanner(modifier = Modifier.statusBarsPadding()) {
                                RoverApplication.APP.dataManger.trackClick("UkraineBanner_Top")
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
                }
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

    @OptIn(KoinExperimentalAPI::class)
    @Composable
    private fun RoversNavDisplay(
        activity: FragmentActivity,
        navState: RoversNavigationState,
        entryDecorators: List<NavEntryDecorator<RoversDestination>>,
        onExit: () -> Unit,
        onHideUi: (Boolean) -> Unit,
    ) {
        val navActions = remember(activity, navState, onHideUi) {
            RoversNavActions(
                activity = activity,
                navState = navState,
                onHideUi = onHideUi,
                onClearCache = ::clearCache,
                onRateApp = ::goToMarket,
            )
        }

        CompositionLocalProvider(LocalRoversNavActions provides navActions) {
            @Suppress("UNCHECKED_CAST")
            val entryProvider = koinEntryProvider() as (RoversDestination) -> NavEntry<RoversDestination>
            NavDisplay(
                backStack = navState.backStack,
                onBack = {
                    if (!navState.pop()) {
                        onExit()
                    }
                },
                entryDecorators = entryDecorators,
                entryProvider = entryProvider
            )
        }
    }

    private fun track(track: String) {
        RoverApplication.APP.dataManger.trackClick(track)
    }

    override fun onResume() {
        super.onResume()
        adView.resume()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(WIDGET_IMAGE_ID_STATE_KEY, widgetImageIdState.value)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        widgetImageIdState.value = intent.getStringExtra(WidgetExtraImageId)
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


@Composable
fun RoversContent(
    rovers: List<Rover>,
    onClick: (rover: Rover) -> Unit,
    onMissionInfoClick: (rover: Rover) -> Unit
) {
    LazyColumn {
        items(rovers) { item ->
            RoverItem(
                rover = item,
                onClick = onClick,
                onMissionInfoClick = onMissionInfoClick
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun RoverItem(
    rover: Rover,
    onClick: (rover: Rover) -> Unit,
    onMissionInfoClick: (rover: Rover) -> Unit
) {
    Timber.d("RoverItem() called with: rover = $rover")
    Box {
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
        androidx.compose.material3.IconButton(
            onClick = { onMissionInfoClick(rover) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            MaterialSymbolIcon(
                symbol = MaterialSymbol.Info,
                contentDescription = "Mission Info"
            )
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
