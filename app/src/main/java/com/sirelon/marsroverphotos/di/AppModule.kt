@file:OptIn(ExperimentalAnimationApi::class, KoinExperimentalAPI::class)

package com.sirelon.marsroverphotos.di

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sirelon.marsroverphotos.RoverApplication
import com.sirelon.marsroverphotos.feature.favorite.FavoriteImagesViewModel
import com.sirelon.marsroverphotos.feature.favorite.FavoriteScreen
import com.sirelon.marsroverphotos.feature.imageIds
import com.sirelon.marsroverphotos.feature.images.ImageScreen
import com.sirelon.marsroverphotos.feature.images.ImageViewModel
import com.sirelon.marsroverphotos.feature.mission.RoverMissionInfoScreen
import com.sirelon.marsroverphotos.feature.mission.RoverMissionInfoViewModel
import com.sirelon.marsroverphotos.feature.photos.PhotosViewModel
import com.sirelon.marsroverphotos.feature.photos.RoverPhotosScreen
import com.sirelon.marsroverphotos.feature.favorite.PopularScreen
import com.sirelon.marsroverphotos.feature.popular.PopularPhotosViewModel
import com.sirelon.marsroverphotos.feature.rovers.LocalRoversNavActions
import com.sirelon.marsroverphotos.feature.rovers.RoversContent
import com.sirelon.marsroverphotos.feature.rovers.RoversDestination
import com.sirelon.marsroverphotos.feature.settings.AboutAppContent
import com.sirelon.marsroverphotos.feature.ukraine.UkraineInfoScreen
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.navigation3.navigation
import org.koin.dsl.module

val appModule = module {
    viewModel { PhotosViewModel(androidApplication()) }
    viewModel { FavoriteImagesViewModel(androidApplication()) }
    viewModel { PopularPhotosViewModel(androidApplication()) }
    viewModel { ImageViewModel(androidApplication()) }
    viewModel { RoverMissionInfoViewModel(androidApplication()) }

    navigation<RoversDestination.Rovers> { _ ->
        val navActions = LocalRoversNavActions.current
        val rovers by RoverApplication.APP.dataManger.rovers.collectAsStateWithLifecycle(
            initialValue = emptyList()
        )

        RoversContent(
            rovers = rovers,
            onClick = {
                RoverApplication.APP.dataManger.trackClick("click_rover_${it.name}")
                navActions.navState.push(RoversDestination.RoverDetail(it.id))
            },
            onMissionInfoClick = {
                RoverApplication.APP.dataManger.trackClick("click_mission_info_${it.name}")
                navActions.navState.push(RoversDestination.MissionInfo(it.id))
            }
        )
    }

    navigation<RoversDestination.About> { _ ->
        val navActions = LocalRoversNavActions.current
        AboutAppContent(
            onClearCache = navActions.onClearCache,
            onRateApp = navActions.onRateApp
        )
    }

    navigation<RoversDestination.Popular> { _ ->
        val navActions = LocalRoversNavActions.current
        PopularScreen(
            onNavigateToImages = { image, photos ->
                navActions.navState.push(
                    RoversDestination.ImageGallery(
                        ids = photos.imageIds(),
                        selectedId = image.id,
                        shouldTrack = false
                    )
                )
            }
        )
    }

    navigation<RoversDestination.Favorite> { _ ->
        val navActions = LocalRoversNavActions.current
        FavoriteScreen(
            onNavigateToImages = { image, photos, tracking ->
                navActions.navState.push(
                    RoversDestination.ImageGallery(
                        ids = photos.imageIds(),
                        selectedId = image.id,
                        shouldTrack = tracking
                    )
                )
            },
            onNavigateToRovers = {
                navActions.navState.selectTopLevel(RoversDestination.Rovers, resetToTop = true)
            }
        )
    }

    navigation<RoversDestination.Ukraine> { _ ->
        UkraineInfoScreen()
    }

    navigation<RoversDestination.RoverDetail> { destination ->
        val navActions = LocalRoversNavActions.current
        RoverPhotosScreen(
            activity = navActions.activity,
            roverId = destination.roverId,
            onNavigateToImages = { image, photos ->
                navActions.navState.push(
                    RoversDestination.ImageGallery(
                        ids = photos.imageIds(),
                        selectedId = image.id,
                        shouldTrack = true
                    )
                )
            }
        )
    }

    navigation<RoversDestination.ImageGallery> { gallery ->
        val navActions = LocalRoversNavActions.current
        ImageScreen(
            trackingEnabled = gallery.shouldTrack,
            photoIds = gallery.ids,
            selectedId = gallery.selectedId,
            onHideUi = navActions.onHideUi,
        )
    }

    navigation<RoversDestination.MissionInfo> { destination ->
        val navActions = LocalRoversNavActions.current
        RoverMissionInfoScreen(
            roverId = destination.roverId,
            onBack = { navActions.navState.pop() }
        )
    }
}
