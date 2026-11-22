package com.sirelon.marsroverphotos.data.support

import com.revenuecat.purchases.kmp.AdTracker
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.models.AdDisplayedData
import com.revenuecat.purchases.kmp.models.AdFailedToLoadData
import com.revenuecat.purchases.kmp.models.AdFormat
import com.revenuecat.purchases.kmp.models.AdLoadedData
import com.revenuecat.purchases.kmp.models.AdMediatorName
import com.revenuecat.purchases.kmp.models.AdOpenedData
import com.revenuecat.purchases.kmp.models.AdRevenueData
import com.revenuecat.purchases.kmp.models.AdRevenuePrecision

/**
 * Reports the AdMob banner's lifecycle to RevenueCat's Ads dashboard.
 *
 * RevenueCat has no AdMob adapter for Kotlin Multiplatform, so the banner callbacks report here by
 * hand: the Android slot directly, the Swift banner through `Main.ios.kt`. The dashboard needs all
 * five events. Revenue alone shows money but no impressions, clicks or fill rate.
 *
 * [impressionId] is AdMob's response id, which ties one load to its display, click and revenue.
 * Every call is dropped until the SDK is configured: the banner can load before the
 * `SupportRepository` single is created.
 */
object RevenueCatAdEvents {

    /** The one banner slot lives in the navigation chrome, shared by every screen. */
    private const val PLACEMENT = "app_chrome"

    fun loaded(adUnitId: String, networkName: String?, impressionId: String) = track {
        trackAdLoaded(
            AdLoadedData(networkName, AdMediatorName.AD_MOB, AdFormat.BANNER, PLACEMENT, adUnitId, impressionId),
        )
    }

    fun displayed(adUnitId: String, networkName: String?, impressionId: String) = track {
        trackAdDisplayed(
            AdDisplayedData(networkName, AdMediatorName.AD_MOB, AdFormat.BANNER, PLACEMENT, adUnitId, impressionId),
        )
    }

    fun opened(adUnitId: String, networkName: String?, impressionId: String) = track {
        trackAdOpened(
            AdOpenedData(networkName, AdMediatorName.AD_MOB, AdFormat.BANNER, PLACEMENT, adUnitId, impressionId),
        )
    }

    fun failedToLoad(adUnitId: String, errorCode: Int?) = track {
        trackAdFailedToLoad(
            AdFailedToLoadData(AdMediatorName.AD_MOB, AdFormat.BANNER, PLACEMENT, adUnitId, errorCode),
        )
    }

    /** [precision] uses the names `Tracker.trackAdImpression` already logs to Firebase. */
    fun revenue(
        adUnitId: String,
        networkName: String?,
        impressionId: String,
        revenueMicros: Long,
        currency: String,
        precision: String,
    ) = track {
        trackAdRevenue(
            AdRevenueData(
                networkName = networkName,
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.BANNER,
                placement = PLACEMENT,
                adUnitId = adUnitId,
                impressionId = impressionId,
                revenueMicros = revenueMicros,
                currency = currency,
                precision = when (precision) {
                    "precise" -> AdRevenuePrecision.EXACT
                    "publisher_provided" -> AdRevenuePrecision.PUBLISHER_DEFINED
                    "estimated" -> AdRevenuePrecision.ESTIMATED
                    else -> AdRevenuePrecision.UNKNOWN
                },
            ),
        )
    }

    private inline fun track(block: AdTracker.() -> Unit) {
        if (Purchases.isConfigured) Purchases.sharedInstance.adTracker.block()
    }
}
