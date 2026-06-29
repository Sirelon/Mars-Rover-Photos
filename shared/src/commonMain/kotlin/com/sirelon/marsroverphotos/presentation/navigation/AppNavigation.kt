package com.sirelon.marsroverphotos.presentation.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.movableContentOf
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.sirelon.marsroverphotos.platform.BuildInfo
import com.sirelon.marsroverphotos.platform.Tracker
import com.sirelon.marsroverphotos.presentation.theme.AppMotion
import com.sirelon.marsroverphotos.presentation.ui.AdSlot
import com.sirelon.marsroverphotos.presentation.ui.UkraineBanner
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import androidx.navigation3.runtime.NavEntry
import com.sirelon.marsroverphotos.di.IMAGES_DESTINATION_KEY
import com.sirelon.marsroverphotos.presentation.screens.DateJumpPickerScreen
import com.sirelon.marsroverphotos.presentation.screens.PhotosFiltersScreen
import com.sirelon.marsroverphotos.presentation.screens.PhotosScreen
import org.koin.compose.koinInject
import org.koin.compose.navigation3.koinEntryProvider
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI

// Screen-slide nav durations (non-Images). Sourced from AppMotion so the whole app shares one timing
// scale; the Images open/close fades use AppMotion.SharedContainerMs to stay locked to the
// shared-element bounds (the open fade lives on the Images nav entry in NavigationModule). See
// docs/DESIGN_SYSTEM.md › Motion.
private const val ANIM_DURATION = AppMotion.ScreenEnterMs

private const val ANIM_DURATION_2 = AppMotion.ScreenExitFadeMs

/** Fade for leaving the fullscreen Images entry — matches the shared-element bounds duration. */
private const val IMAGES_POP_FADE = AppMotion.SharedContainerMs

/**
 * Main Navigation 3 display for the app.
 */
@OptIn(KoinExperimentalAPI::class, ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    startDestination: AppDestination = AppDestination.Rovers,
    deepLink: DeepLink? = null,
    onDeepLinkConsumed: (() -> Unit)? = null
) {
    // Nav3 + Desktop: providing `elements` to rememberNavBackStack causes the start
    // destination to be added twice when saved-state restoration also returns the same
    // key — `SaveableStateHolder` then throws "Key used multiple times". Fix: start
    // with an empty back stack and add the start destination only when truly empty.
    val backStack = rememberNavBackStack(configuration = navBackStackConfiguration)
    remember(backStack) {
        if (backStack.isEmpty()) backStack.add(startDestination)
    }

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
                    onOpenDateJumpPicker = {
                        navigator.navigate(AppDestination.PhotosDateJumpPicker(key.roverId))
                    },
                    onOpenFilters = {
                        navigator.navigate(AppDestination.PhotosFilters(key.roverId))
                    },
                )
            }

            is AppDestination.PhotosDateJumpPicker -> NavEntry<NavKey>(
                key = key,
                contentKey = "${photosContentKey(key.roverId)}/date-jump",
                metadata = SharedViewModelStoreNavEntryDecorator.parent(photosContentKey(key.roverId)) +
                    DialogOverlaySceneStrategy.dialogOverlay(),
            ) {
                DateJumpPickerScreen(
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
                    onOpenDateJumpPicker = {
                        navigator.replaceTop(AppDestination.PhotosDateJumpPicker(key.roverId))
                    },
                )
            }

            else -> koinProvider(key)
        }
    }
    val dialogOverlaySceneStrategy = remember { DialogOverlaySceneStrategy<NavKey>() }
    val tracker: Tracker = koinInject()
    val isImages = chromeDestination is AppDestination.Images

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

    // movableContentOf prevents NavDisplay from being disposed+remounted when the adaptive
    // layout branch switches (NavBar ↔ NavRail on Desktop window resize / initial render).
    // Without this, SaveableStateHolder registers the same key twice during applyLateChanges.
    val navDisplay = remember {
      movableContentOf {
        SharedTransitionLayout {
          CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavDisplay(
                backStack = backStack,
                onBack = { navigator.goBack() },
                entryDecorators = entryDecorators,
                sceneStrategies = listOf(dialogOverlaySceneStrategy),
                entryProvider = entryProvider,
                // Forward navigation: new screen slides in from the right
                transitionSpec = {
                    (slideInHorizontally(tween(ANIM_DURATION)) { it } + fadeIn(tween(ANIM_DURATION))) togetherWith
                        (slideOutHorizontally(tween(ANIM_DURATION)) { -it / 5 } + fadeOut(tween(ANIM_DURATION_2)))
                },
                // Standard back: previous screen revealed from the left.
                // When leaving Images, suppress the slide — shared elements handle the visual.
                popTransitionSpec = {
                    if (initialState.metadata[IMAGES_DESTINATION_KEY] == true) {
                        EnterTransition.None togetherWith fadeOut(tween(IMAGES_POP_FADE, easing = AppMotion.Emphasized))
                    } else {
                        (slideInHorizontally(tween(ANIM_DURATION)) { -it / 5 } + fadeIn(tween(ANIM_DURATION))) togetherWith
                            (slideOutHorizontally(tween(ANIM_DURATION)) { it } + fadeOut(tween(ANIM_DURATION_2)))
                    }
                },
                // Predictive back: same curve — NavDisplay drives this interactively
                // with the system's back gesture progress on Android 14+
                predictivePopTransitionSpec = {
                    if (initialState.metadata[IMAGES_DESTINATION_KEY] == true) {
                        EnterTransition.None togetherWith fadeOut(tween(IMAGES_POP_FADE, easing = AppMotion.Emphasized))
                    } else {
                        (slideInHorizontally(tween(ANIM_DURATION)) { -it / 5 } + fadeIn(tween(ANIM_DURATION))) togetherWith
                            (slideOutHorizontally(tween(ANIM_DURATION)) { it } + fadeOut(tween(ANIM_DURATION_2)))
                    }
                },
            )
          }
        }
      }
    }

    // One persistent layout: the Images screen doesn't swap containers (that made the chrome
    // pop in after the close animation). Instead the chrome — nav bar/rail, ad slot, banner,
    // status-bar inset — animates out when Images opens and back in alongside the pop fade,
    // so the NavDisplay container resizes smoothly in both directions.
    CompositionLocalProvider(LocalAppNavigator provides navigator) {
        Box(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
            MarsNavigationSuite(
                modifier = Modifier.fillMaxSize(),
                selectedDestination = chromeDestination.topLevelDestination(),
                onDestinationClick = { destination ->
                    tracker.trackClick("nav_${destination.analyticsTag}")
                    navigator.selectTopLevel(destination)
                },
                resetScrollKey = chromeDestination,
                chromeVisible = !isImages,
                bottomChrome = { if (!BuildInfo.hideAds) AdSlot(modifier = Modifier.fillMaxWidth()) },
            ) {
                val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                val topPadding by animateDpAsState(
                    targetValue = if (isImages) 0.dp else statusBarTop,
                    animationSpec = tween(if (isImages) CHROME_HIDE_MS else CHROME_SHOW_MS),
                    label = "statusBarPadding",
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topPadding)
                        // Consume exactly the inset that was applied: screens that add their own
                        // statusBarsPadding (e.g. fullscreen Images) see only the remainder, so
                        // there's no double inset after the chrome animates back in.
                        .consumeWindowInsets(PaddingValues(top = topPadding)),
                ) {
                    AnimatedVisibility(chromeDestination !is AppDestination.Ukraine && !isImages) {
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
                            .background(MaterialTheme.colorScheme.background),
                    ) {
                        navDisplay()
                    }
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
        AppDestination.AdminPhotos,
        is AppDestination.PhotosDateJumpPicker,
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
            subclass(AppDestination.PhotosDateJumpPicker::class, AppDestination.PhotosDateJumpPicker.serializer())
            subclass(AppDestination.PhotosFilters::class, AppDestination.PhotosFilters.serializer())
            subclass(AppDestination.AdminPhotos::class, AppDestination.AdminPhotos.serializer())
        }
    }
}
