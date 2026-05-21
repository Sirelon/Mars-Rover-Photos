package com.sirelon.marsroverphotos.presentation.ui

import android.os.Bundle
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.sirelon.marsroverphotos.platform.AndroidAdConsent

private const val BANNER_AD_UNIT_ID = "ca-app-pub-7516059448019339/9309101894"

@Composable
actual fun AdSlot(modifier: Modifier) {
    val context = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    val lifecycleOwner = LocalLifecycleOwner.current
    val personalizedAds by AndroidAdConsent.personalizedAds.collectAsState()

    val adView = remember(context, widthDp) {
        AdView(context).apply {
            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp))
            adUnitId = BANNER_AD_UNIT_ID
        }
    }

    LaunchedEffect(adView, personalizedAds) {
        adView.loadAd(buildAdRequest(personalizedAds))
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
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adView.destroy()
        }
    }

    Box(modifier = modifier) {
        AndroidView(factory = { adView })
    }
}

private fun buildAdRequest(personalized: Boolean): AdRequest {
    val builder = AdRequest.Builder()
    if (!personalized) {
        val extras = Bundle().apply { putString("npa", "1") }
        builder.addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
    }
    return builder.build()
}
