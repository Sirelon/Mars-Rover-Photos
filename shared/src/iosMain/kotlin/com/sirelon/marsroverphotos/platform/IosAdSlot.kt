@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.sirelon.marsroverphotos.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import platform.UIKit.UIView

/**
 * Native iOS factory for the AdMob banner.
 *
 * Implemented in Swift (see `iosApp/iosApp/BannerAdFactory.swift`) where the GoogleMobileAds SDK
 * lives, and registered with [IosAdSlot] at app launch from `MarsRoverApp.swift`.
 */
interface AdBannerFactory {
    fun createBanner(widthPoints: Double): UIView
}

/**
 * Registry the iOS app shell uses to hand a real `GADBannerView`-producing factory into the
 * shared Compose AdSlot. Stays `null` on Desktop and Android.
 */
object IosAdSlot {
    var factory: AdBannerFactory? by mutableStateOf(null)
}
