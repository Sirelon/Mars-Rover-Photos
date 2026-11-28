package com.sirelon.marsroverphotos.platform

import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.sirelon.marsroverphotos.utils.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Starts the Google Mobile Ads SDK the first time the banner may request an ad, never at process
 * start.
 *
 * `MobileAds.initialize()` runs on a worker thread, as Google recommends, and it brings up WebView
 * there. Off the main thread, WebView's first start posts work to the main thread and waits for it.
 * If the main thread meanwhile calls into the SDK, it blocks on the SDK's init lock: a deadlock and
 * an ANR. Google's ad inspector does exactly that when it launches the process straight into the
 * SDK's `OutOfContextTestingActivity` (Crashlytics issue `MarsRoverApplication$onCreate$7`).
 * Starting the SDK here, and creating the banner only after [initialized], keeps every main-thread
 * SDK call out of the init window. Ad-free users never start the SDK at all.
 */
object AdMobSdk {

    private const val TAG = "AdMobSdk"

    /** Test devices, so debug ad clicks don't count as invalid traffic (id from the SDK log). */
    private val TEST_DEVICE_IDS = listOf("5E9A79263E2CEF0CABB3EB5C02E071D0")

    private val started = AtomicBoolean(false)
    private val _initialized = MutableStateFlow(false)

    /** True once the SDK finished initializing; the banner may be created and loaded from then on. */
    val initialized: StateFlow<Boolean> = _initialized.asStateFlow()

    /** Idempotent: only the first call starts the SDK. */
    fun initialize(context: Context) {
        if (!started.compareAndSet(false, true)) return
        val appContext = context.applicationContext
        thread(name = "AdMobInit", isDaemon = true) {
            try {
                if (BuildInfo.isDebug) {
                    MobileAds.setRequestConfiguration(
                        RequestConfiguration.Builder().setTestDeviceIds(TEST_DEVICE_IDS).build()
                    )
                }
                MobileAds.initialize(appContext) { status ->
                    Logger.d(TAG) { "AdMob init status: ${status.adapterStatusMap}" }
                    _initialized.value = true
                }
            } catch (e: Exception) {
                Logger.w(TAG) { "AdMob initialization failed: ${e.message}" }
            }
        }
    }
}
