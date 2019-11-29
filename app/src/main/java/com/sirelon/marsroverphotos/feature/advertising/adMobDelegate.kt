package com.sirelon.marsroverphotos.feature.advertising

import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.NativeExpressAdView
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.adapter.AdapterConstants
import com.sirelon.marsroverphotos.models.ViewType

/**
 * Created on 2/11/18 12:57 for Mars-Rover-Photos.
 */
internal object adMobDelegate : AdvertisingDelegate {

    override fun loadAd(adView: NativeExpressAdView) {
        initializate(adView.context)
        adView.loadAd(AdRequest.Builder().build())
    }

    override fun integregrateAdToList(
        dataList: List<ViewType>
    ): MutableList<ViewType> {

dataList.chunked(30).map { it.toMutableList().add(createAdverizingItem()) }

        var step = 30

        val data: MutableList<ViewType> = dataList.toMutableList()

        if (step >= data.size / 2) {
            data.add(data.size, createAdverizingItem())
            return data
        }

        if (step >= data.size) {
            step = data.size / 2 + 1
        }

        IntProgression.fromClosedRange(step, data.size, step)
            .map { data.add(it, createAdverizingItem()) }
        return data
    }

    private fun createAdverizingItem(): ViewType {
        return object : ViewType {
            override fun getViewId() = this.hashCode()

            override fun getViewType(): Int = AdapterConstants.ADVERTIZING
        }
    }

    private fun initializate(context: Context) {
        MobileAds.initialize(context, context.getString(R.string.ad_application_id))
    }

}