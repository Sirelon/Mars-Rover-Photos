package com.sirelon.marsroverphotos.feature.advertising

import com.google.android.gms.ads.NativeExpressAdView
import com.sirelon.marsroverphotos.models.ViewType

/**
 * Created on 2/11/18 12:56 for Mars-Rover-Photos.
 */
interface AdvertisingDelegate {

    fun integregrateAdToList(dataList: List<out ViewType>): List<out ViewType>
    fun loadAd(adView: NativeExpressAdView)
}