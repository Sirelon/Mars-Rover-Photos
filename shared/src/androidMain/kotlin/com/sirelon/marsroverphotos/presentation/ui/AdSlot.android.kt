package com.sirelon.marsroverphotos.presentation.ui

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.sirelon.marsroverphotos.platform.AndroidAdConsent
import com.sirelon.marsroverphotos.platform.Tracker
import org.koin.compose.koinInject

private const val BANNER_AD_UNIT_ID = "ca-app-pub-7516059448019339/9309101894"
private const val AD_FORMAT = "banner"

/** The Android SDK reports an impression's worth in millionths of a currency unit. */
private const val MICROS_PER_CURRENCY_UNIT = 1_000_000.0

@Composable
actual fun AdSlot(modifier: Modifier) {
    val context = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    val lifecycleOwner = LocalLifecycleOwner.current
    val tracker: Tracker = koinInject()
    val canRequestAds by AndroidAdConsent.canRequestAds.collectAsState()
    val personalizedAds by AndroidAdConsent.personalizedAds.collectAsState()

    val adView = remember(context, widthDp, tracker) {
        RetainedBanner.obtain(context, widthDp, lifecycleOwner.lifecycle, tracker)
    }

    LaunchedEffect(adView, canRequestAds, personalizedAds) {
        if (!canRequestAds) return@LaunchedEffect
        RetainedBanner.load(personalizedAds)
    }

    DisposableEffect(lifecycleOwner, adView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> adView.resume()
                Lifecycle.Event.ON_PAUSE -> adView.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        // Re-entering composition mid-session: the retained view was paused on the way out.
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) adView.resume()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // Collapsing chrome is not the ad's death. Stop the refresh timer so nothing is
            // requested or counted off screen, hand the view back, but keep it loaded.
            adView.pause()
            adView.detachFromParent()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = {
                // The same instance comes back on every re-entry, and AndroidView's holder refuses
                // a child that still has a parent.
                adView.detachFromParent()
                adView
            },
        )
    }
}

/**
 * Keeps the banner alive across the composable that shows it.
 *
 * [AdSlot] is navigation chrome, so it leaves composition every time a fullscreen destination
 * collapses the chrome — which on this app's hottest path is every photo the user opens and closes.
 * Destroying the [AdView] there throws away an impression that has already been paid for and makes
 * the chrome come back to an empty slot and a fresh ad request. The view is retained here instead,
 * paused while off screen, and re-attached with its ad still loaded.
 *
 * It is rebuilt when the Activity or the width it was sized for changes, and destroyed with the
 * Activity it belongs to. That observer lives here rather than in the composable because the
 * composable is absent for exactly the window in which the Activity can go away unnoticed.
 */
private object RetainedBanner {

    private var view: AdView? = null
    private var owner: Context? = null
    private var widthDp = 0

    /** Personalization the loaded ad was requested with; `null` while a request is due. */
    private var loadedPersonalized: Boolean? = null

    fun obtain(context: Context, widthDp: Int, lifecycle: Lifecycle, tracker: Tracker): AdView {
        val current = view
        if (current != null && owner === context && this.widthDp == widthDp) return current

        release()
        val created = AdView(context)
        created.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp))
        created.adUnitId = BANNER_AD_UNIT_ID
        created.onPaidEventListener = paidEventListener(created, tracker)
        created.adListener = adListener(tracker)
        view = created
        owner = context
        this.widthDp = widthDp

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                lifecycle.removeObserver(this)
                // Anything that replaced this view already destroyed it on the way past.
                if (view === created) release()
            }
        })
        return created
    }

    /** No-op unless the banner has never loaded, or consent changed under the loaded ad. */
    fun load(personalized: Boolean) {
        val current = view ?: return
        if (loadedPersonalized == personalized) return
        loadedPersonalized = personalized
        current.loadAd(buildAdRequest(personalized))
    }

    private fun release() {
        view?.let {
            it.detachFromParent()
            it.destroy()
        }
        view = null
        owner = null
        loadedPersonalized = null
    }

    /**
     * Impression-level revenue. This is the only place the SDK reports what an ad was actually
     * worth — the AdMob console reports it per day and per unit, never per user or per session.
     */
    private fun paidEventListener(adView: AdView, tracker: Tracker) = OnPaidEventListener { adValue ->
        tracker.trackAdImpression(
            adSource = adView.responseInfo?.loadedAdapterResponseInfo?.adSourceName ?: "unknown",
            adFormat = AD_FORMAT,
            adUnitName = BANNER_AD_UNIT_ID,
            value = adValue.valueMicros / MICROS_PER_CURRENCY_UNIT,
            currencyCode = adValue.currencyCode,
            precision = adValue.precisionName(),
        )
    }

    /** Loads and failures are the fill rate; without both, an empty slot looks like no traffic. */
    private fun adListener(tracker: Tracker) = object : AdListener() {
        override fun onAdLoaded() {
            tracker.trackEvent("ad_loaded", mapOf("ad_format" to AD_FORMAT))
        }

        override fun onAdFailedToLoad(error: LoadAdError) {
            // Let the next entry retry: otherwise one flaky request leaves the slot empty for the
            // rest of the session, because the retained view never asks again.
            loadedPersonalized = null
            tracker.trackEvent(
                "ad_load_failed",
                mapOf(
                    "ad_format" to AD_FORMAT,
                    "error_code" to error.code.toString(),
                    // GA4 truncates string params at 100 chars, and the head carries the cause.
                    "reason" to error.message.take(100),
                ),
            )
        }
    }
}

private fun AdView.detachFromParent() {
    (parent as? ViewGroup)?.removeView(this)
}

private fun AdValue.precisionName(): String = when (precisionType) {
    AdValue.PrecisionType.ESTIMATED -> "estimated"
    AdValue.PrecisionType.PUBLISHER_PROVIDED -> "publisher_provided"
    AdValue.PrecisionType.PRECISE -> "precise"
    else -> "unknown"
}

private fun buildAdRequest(personalized: Boolean): AdRequest {
    val builder = AdRequest.Builder()
    if (!personalized) {
        val extras = Bundle().apply { putString("npa", "1") }
        builder.addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
    }
    return builder.build()
}
