@file:OptIn(KoinExperimentalAPI::class)

package com.sirelon.marsroverphotos.di

import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.sirelon.marsroverphotos.presentation.navigation.AppDestination
import com.sirelon.marsroverphotos.presentation.navigation.LocalAppNavigator
import com.sirelon.marsroverphotos.presentation.navigation.LocalPhotosViewModelStoreOwners
import com.sirelon.marsroverphotos.presentation.screens.AboutScreen
import com.sirelon.marsroverphotos.presentation.screens.EarthDatePickerScreen
import com.sirelon.marsroverphotos.presentation.screens.FavoriteScreen
import com.sirelon.marsroverphotos.presentation.screens.ImagesScreen
import com.sirelon.marsroverphotos.presentation.screens.PhotosScreen
import com.sirelon.marsroverphotos.presentation.screens.PopularScreen
import com.sirelon.marsroverphotos.presentation.screens.RoverMissionInfoScreen
import com.sirelon.marsroverphotos.presentation.screens.RoversScreen
import com.sirelon.marsroverphotos.presentation.screens.SolPickerScreen
import com.sirelon.marsroverphotos.presentation.screens.UkraineScreen
import com.sirelon.marsroverphotos.presentation.viewmodels.PhotosViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

val navigationModule = module {
    navigation<AppDestination.Rovers> {
        val navigator = LocalAppNavigator.current
        RoversScreen(
            onNavigateToPhotos = { roverId ->
                navigator.navigate(AppDestination.Photos(roverId))
            },
            onMissionInfoClick = { roverId ->
                navigator.navigate(AppDestination.Mission(roverId))
            }
        )
    }

    navigation<AppDestination.Photos> { destination ->
        val navigator = LocalAppNavigator.current

        // Publish this Photos entry's ViewModelStoreOwner so its dialog entries can
        // resolve the SAME PhotosViewModel instance. Inside the entry content,
        // LocalViewModelStoreOwner is the per-entry store on Android (and the shared
        // root owner on iOS/Desktop) — exactly the store koinViewModel() uses below.
        // Registered synchronously during composition (not in an effect): Photos is
        // always composed before its dialog (it sits lower in the back stack), so the
        // owner is present when the dialog reads it — even on state restoration.
        val photosStoreOwners = LocalPhotosViewModelStoreOwners.current
        val entryOwner = LocalViewModelStoreOwner.current
        if (entryOwner != null) {
            photosStoreOwners[destination.roverId] = entryOwner
        }
        DisposableEffect(destination.roverId) {
            onDispose { photosStoreOwners.remove(destination.roverId) }
        }

        PhotosScreen(
            roverId = destination.roverId,
            cameraFilter = destination.camera,
            onNavigateToImages = { photoId ->
                navigator.navigate(
                    AppDestination.Images(
                        photoIds = listOf(photoId),
                        selectedId = photoId,
                        source = AppDestination.ImagesSource.DIRECT_IDS,
                    )
                )
            },
            onClearCameraFilter = {
                navigator.replaceTop(AppDestination.Photos(destination.roverId, camera = null))
            },
            onBack = { navigator.goBack() },
            onOpenSolPicker = {
                navigator.navigate(AppDestination.PhotosSolPicker(destination.roverId))
            },
            onOpenEarthDatePicker = {
                navigator.navigate(AppDestination.PhotosEarthDatePicker(destination.roverId))
            },
        )
    }

    navigation<AppDestination.PhotosSolPicker> { destination ->
        val navigator = LocalAppNavigator.current
        // Resolve the SAME PhotosViewModel instance the Photos screen uses by pointing
        // koinViewModel at the Photos entry's ViewModelStoreOwner (registered above).
        // Fallback to the current owner keeps it safe if the parent isn't found.
        val sharedOwner = LocalPhotosViewModelStoreOwners.current[destination.roverId]
            ?: LocalViewModelStoreOwner.current!!
        val viewModel = koinViewModel<PhotosViewModel>(viewModelStoreOwner = sharedOwner)
        SolPickerScreen(
            viewModel = viewModel,
            onDismiss = { navigator.goBack() },
        )
    }

    navigation<AppDestination.PhotosEarthDatePicker> { destination ->
        val navigator = LocalAppNavigator.current
        val sharedOwner = LocalPhotosViewModelStoreOwners.current[destination.roverId]
            ?: LocalViewModelStoreOwner.current!!
        val viewModel = koinViewModel<PhotosViewModel>(viewModelStoreOwner = sharedOwner)
        EarthDatePickerScreen(
            viewModel = viewModel,
            onDismiss = { navigator.goBack() },
        )
    }

    navigation<AppDestination.Images> { destination ->
        val navigator = LocalAppNavigator.current
        ImagesScreen(
            photoIds = destination.photoIds,
            selectedId = destination.selectedId,
            source = destination.source,
            onBack = { navigator.goBack() }
        )
    }

    navigation<AppDestination.Favorite> {
        val navigator = LocalAppNavigator.current
        FavoriteScreen(
            onNavigateToImages = { image ->
                navigator.navigate(
                    AppDestination.Images(
                        selectedId = image.id,
                        source = AppDestination.ImagesSource.FAVORITES,
                    )
                )
            },
            onNavigateToRovers = { navigator.selectTopLevel(AppDestination.Rovers) }
        )
    }

    navigation<AppDestination.Popular> {
        val navigator = LocalAppNavigator.current
        PopularScreen(
            onNavigateToImages = { image ->
                navigator.navigate(
                    AppDestination.Images(
                        selectedId = image.id,
                        source = AppDestination.ImagesSource.POPULAR,
                    )
                )
            }
        )
    }

    navigation<AppDestination.Mission> { destination ->
        val navigator = LocalAppNavigator.current
        RoverMissionInfoScreen(
            roverId = destination.roverId,
            onBack = { navigator.goBack() },
            onCameraClick = { cameraAbbrev ->
                navigator.navigate(
                    AppDestination.Photos(
                        roverId = destination.roverId,
                        camera = cameraAbbrev
                    )
                )
            }
        )
    }

    navigation<AppDestination.About> {
        AboutScreen()
    }

    navigation<AppDestination.Ukraine> {
        val navigator = LocalAppNavigator.current
        UkraineScreen(onBack = { navigator.goBack() })
    }
}
