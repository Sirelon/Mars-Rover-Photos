package com.sirelon.marsroverphotos.presentation.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.sirelon.marsroverphotos.platform.Tracker
import com.sirelon.marsroverphotos.presentation.ui.AdSlot
import com.sirelon.marsroverphotos.presentation.ui.UkraineBanner
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import androidx.navigation3.runtime.NavEntry
import com.sirelon.marsroverphotos.presentation.screens.EarthDatePickerScreen
import com.sirelon.marsroverphotos.presentation.screens.PhotosFiltersScreen
import com.sirelon.marsroverphotos.presentation.screens.PhotosScreen
import com.sirelon.marsroverphotos.presentation.screens.SolPickerScreen
import org.koin.compose.koinInject
import org.koin.compose.navigation3.koinEntryProvider
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI

private const val ANIM_DURATION = 600

private const val ANIM_DURATION_2 = ANIM_DURATION / 2

/**
 * Main Navigation 3 display for the app.
 */
@OptIn(KoinExperimentalAPI::class, ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    startDestination: AppDestination = AppDestination.Rovers,
    deepLink: DeepLink? = null,
    onDeepLinkConsumed: (() -> Unit)? = null
) {
    val backStack = rememberNavBackStack(
        configuration = navBackStackConfiguration,
        elements = arrayOf(startDestination)
    )
    val navigator = remember(backStack) { AppNavigator(backStack) }
    val entryDecorators = rememberAppNavEntryDecorators()
    val currentDestination = backStack.lastOrNull() as? AppDestination ?: startDestination
    val chromeDestination = backStack.lastOrNull { it !is AppDestination.DialogDestination } as? AppDestination
        ?: currentDestination
    // The Photos flow (screen + Sol/Earth dialogs) shares one PhotosViewModel via the Photos
    // entry's ViewModelStore. The Photos entry uses a camera-independent contentKey so the VM
    // survives camera-filter changes; the dialog entries name it as their parent and resolve the
    // same PhotosViewModel through LocalSharedViewModelStoreOwner. These three are declared as raw
    // NavEntry (Koin's navigation<> DSL can't set contentKey); everything else comes from Koin.
    val koinProvider = koinEntryProvider<NavKey>()
    val entryProvider: (NavKey) -> NavEntry<NavKey> = { key ->
        when (key) {
            is AppDestination.Photos -> NavEntry<NavKey>(
                key = key,
                contentKey = photosContentKey(key.roverId),
            ) {
                PhotosScreen(
                    roverId = key.roverId,
                    cameraFilter = key.camera,
                    onNavigateToImages = { clickedId, cameras ->
                        navigator.navigate(
                            AppDestination.Images(
                                selectedId = clickedId,
                                roverId = key.roverId,
                                camera = key.camera,
                                cameras = cameras,
                                source = AppDestination.ImagesSource.ROVER_FEED,
                            )
                        )
                    },
                    onClearCameraFilter = {
                        navigator.replaceTop(AppDestination.Photos(key.roverId, camera = null))
                    },
                    onBack = { navigator.goBack() },
                    onOpenSolPicker = {
                        navigator.navigate(AppDestination.PhotosSolPicker(key.roverId))
                    },
                    onOpenEarthDatePicker = {
                        navigator.navigate(AppDestination.PhotosEarthDatePicker(key.roverId))
                    },
                    onOpenFilters = {
                        navigator.navigate(AppDestination.PhotosFilters(key.roverId))
                    },
                )
            }

            is AppDestination.PhotosSolPicker -> NavEntry<NavKey>(
                key = key,
                contentKey = "${photosContentKey(key.roverId)}/sol",
                metadata = SharedViewModelStoreNavEntryDecorator.parent(photosContentKey(key.roverId)) +
                    DialogOverlaySceneStrategy.dialogOverlay(),
            ) {
                SolPickerScreen(
                    viewModel = koinViewModel(viewModelStoreOwner = LocalSharedViewModelStoreOwner.current),
                    onDismiss = { navigator.goBack() },
                )
            }

            is AppDestination.PhotosEarthDatePicker -> NavEntry<NavKey>(
                key = key,
                contentKey = "${photosContentKey(key.roverId)}/earth",
                metadata = SharedViewModelStoreNavEntryDecorator.parent(photosContentKey(key.roverId)) +
                    DialogOverlaySceneStrategy.dialogOverlay(),
            ) {
                EarthDatePickerScreen(
                    viewModel = koinViewModel(viewModelStoreOwner = LocalSharedViewModelStoreOwner.current),
                    onDismiss = { navigator.goBack() },
                )
            }

            is AppDestination.PhotosFilters -> NavEntry<NavKey>(
                key = key,
                contentKey = "${photosContentKey(key.roverId)}/filters",
                metadata = SharedViewModelStoreNavEntryDecorator.parent(photosContentKey(key.roverId)) +
                    DialogOverlaySceneStrategy.dialogOverlay(),
            ) {
                PhotosFiltersScreen(
                    viewModel = koinViewModel(viewModelStoreOwner = LocalSharedViewModelStoreOwner.current),
                    roverId = key.roverId,
                    onDismiss = { navigator.goBack() },
                    onOpenSolPicker = {
                        navigator.replaceTop(AppDestination.PhotosSolPicker(key.roverId))
                    },
                    onOpenEarthDatePicker = {
                        navigator.replaceTop(AppDestination.PhotosEarthDatePicker(key.roverId))
                    },
                )
            }

            else -> koinProvider(key)
        }
    }
    val dialogOverlaySceneStrategy = remember { DialogOverlaySceneStrategy<NavKey>() }
    val tracker: Tracker = koinInject()
    val isImages = chromeDestination is AppDestination.Images
    val bottomBarScrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()

    // Reset the bottom bar to visible whenever the user navigates to a new top-level destination.
    LaunchedEffect(chromeDestination) {
        bottomBarScrollBehavior.state.heightOffset = 0f
    }

    LaunchedEffect(deepLink) {
        val target = deepLink ?: return@LaunchedEffect
        when (target) {
            is DeepLink.Rover -> navigator.navigate(AppDestination.Photos(target.id))
            is DeepLink.Photo -> navigator.navigate(
                AppDestination.Images(
                    photoIds = listOf(target.id.toString()),
                    selectedId = target.id.toString()
                )
            )

            is DeepLink.Image -> navigator.navigate(
                AppDestination.Images(
                    photoIds = listOf(target.id),
                    selectedId = target.id
                )
            )
        }
        onDeepLinkConsumed?.invoke()
    }

    CompositionLocalProvider(LocalAppNavigator provides navigator) {
        // Images screen goes fully edge-to-edge: no status-bar inset, no bottom chrome.
        Column(
            modifier = if (isImages) modifier
            else modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .nestedScroll(bottomBarScrollBehavior.nestedScrollConnection)
        ) {
            AnimatedVisibility(!isImages && chromeDestination !is AppDestination.Ukraine) {
                UkraineBanner(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        tracker.trackClick("UkraineBanner_Root")
                        navigator.navigate(AppDestination.Ukraine)
                    },
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    // Opaque background prevents previous navigation entries from
                    // showing through during transitions or when a screen composable
                    // doesn't fill its entire allocated area (esp. visible on iOS).
                    .background(MaterialTheme.colorScheme.background)
            ) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { navigator.goBack() },
                    entryDecorators = entryDecorators,
                    sceneStrategies = listOf(dialogOverlaySceneStrategy),
                    entryProvider = entryProvider,
                    // Forward navigation: new screen slides in from the right
                    transitionSpec = {
                        (slideInHorizontally(tween(ANIM_DURATION)) { it } + fadeIn(
                            tween(
                                ANIM_DURATION
                            )
                        )) togetherWith
                                (slideOutHorizontally(tween(ANIM_DURATION)) { -it / 5 } + fadeOut(
                                    tween(
                                        ANIM_DURATION_2
                                    )
                                ))
                    },
                    // Standard back: previous screen revealed from the left
                    popTransitionSpec = {
                        (slideInHorizontally(tween(ANIM_DURATION)) { -it / 5 } + fadeIn(
                            tween(
                                ANIM_DURATION
                            )
                        )) togetherWith
                                (slideOutHorizontally(tween(ANIM_DURATION)) { it } + fadeOut(
                                    tween(
                                        ANIM_DURATION_2
                                    )
                                ))
                    },
                    // Predictive back: same curve — NavDisplay drives this interactively
                    // with the system's back gesture progress on Android 14+
                    predictivePopTransitionSpec = {
                        (slideInHorizontally(tween(ANIM_DURATION)) { -it / 5 } + fadeIn(
                            tween(ANIM_DURATION)
                        )) togetherWith
                                (slideOutHorizontally(tween(ANIM_DURATION)) { it } + fadeOut(
                                    tween(ANIM_DURATION_2)
                                ))
                    },
                )
            }
            if (!isImages) {
                // Wrap in a column that always pads for the system navigation bar.
                // BottomAppBar's own windowInsets are cleared to prevent double-padding.
                Column(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
                    AdSlot(modifier = Modifier.fillMaxWidth())
                    MarsBottomBar(
                        selectedDestination = chromeDestination.topLevelDestination(),
                        onDestinationClick = { destination ->
                            tracker.trackClick("bottom_${destination.analyticsTag}")
                            navigator.selectTopLevel(destination)
                        },
                        scrollBehavior = bottomBarScrollBehavior,
                        windowInsets = WindowInsets(),
                    )
                }
            }
        }
    }
}

/**
 * Stable, camera-independent contentKey for a rover's Photos entry. Used as the Photos
 * entry's contentKey and named by its dialog entries as their shared-ViewModelStore parent.
 */
private fun photosContentKey(roverId: Long): String = "photos-$roverId"

private fun AppDestination.topLevelDestination(): AppDestination {
    return when (this) {
        AppDestination.Rovers,
        is AppDestination.Photos,
        is AppDestination.Images,
        is AppDestination.Mission,
        AppDestination.Ukraine,
        is AppDestination.PhotosSolPicker,
        is AppDestination.PhotosEarthDatePicker,
        is AppDestination.PhotosFilters -> AppDestination.Rovers

        AppDestination.Favorite -> AppDestination.Favorite
        AppDestination.Popular -> AppDestination.Popular
        AppDestination.About -> AppDestination.About
    }
}

private val AppDestination.analyticsTag: String
    get() = when (this) {
        AppDestination.Rovers -> "rovers"
        AppDestination.Favorite -> "favorite"
        AppDestination.Popular -> "popular"
        AppDestination.About -> "about"
        AppDestination.Ukraine -> "ukraine"
        is AppDestination.PhotosFilters -> "photos_filters"
        else -> "other"
    }

private val navBackStackConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(AppDestination.Rovers::class, AppDestination.Rovers.serializer())
            subclass(AppDestination.Photos::class, AppDestination.Photos.serializer())
            subclass(AppDestination.Images::class, AppDestination.Images.serializer())
            subclass(AppDestination.Favorite::class, AppDestination.Favorite.serializer())
            subclass(AppDestination.Popular::class, AppDestination.Popular.serializer())
            subclass(AppDestination.Mission::class, AppDestination.Mission.serializer())
            subclass(AppDestination.About::class, AppDestination.About.serializer())
            subclass(AppDestination.Ukraine::class, AppDestination.Ukraine.serializer())
            subclass(AppDestination.PhotosSolPicker::class, AppDestination.PhotosSolPicker.serializer())
            subclass(AppDestination.PhotosEarthDatePicker::class, AppDestination.PhotosEarthDatePicker.serializer())
            subclass(AppDestination.PhotosFilters::class, AppDestination.PhotosFilters.serializer())
        }
    }
}
