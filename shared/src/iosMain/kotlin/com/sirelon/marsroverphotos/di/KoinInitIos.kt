package com.sirelon.marsroverphotos.di

import com.sirelon.marsroverphotos.domain.repositories.RoversRepository
import com.sirelon.marsroverphotos.domain.repositories.SupportRepository
import com.sirelon.marsroverphotos.platform.BuildInfo
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.mp.KoinPlatform
import platform.Foundation.NSBundle

object IosApp {
    private const val AD_FREE_WAIT_MS = 1_500L

    /**
     * @param revenueCatApiKey public RevenueCat SDK key — the App Store key in release, the Test
     *   Store key in debug; chosen by the Swift `#if DEBUG` so the test key never routes in release.
     */
    fun start(isDebug: Boolean, revenueCatApiKey: String) {
        BuildInfo.init(
            versionName = NSBundle.mainBundle.infoDictionary
                ?.get("CFBundleShortVersionString") as? String ?: "unknown",
            isDebug = isDebug,
            packageName = NSBundle.mainBundle.bundleIdentifier ?: "com.sirelon.marsroverphotos"
        )

        val koinApplication = initKoin(listOf(platformModule, supportModule(revenueCatApiKey)))
        koinApplication.koin.get<RoversRepository>().initialize()
        // Configures the billing SDK now, so its cached entitlement is ready before the ad bootstrap.
        koinApplication.koin.get<SupportRepository>()
    }

    /**
     * Whether the user owns a Support tier, for the Swift-side ads bootstrap: supporters get no
     * consent prompt, no ATT prompt and no ad SDK start. The value comes from the billing SDK's local
     * cache and is normally there in milliseconds; the wait is bounded so a first launch without a
     * cache never delays ads for more than [AD_FREE_WAIT_MS].
     */
    suspend fun isAdFree(): Boolean {
        val repository = KoinPlatform.getKoin().get<SupportRepository>()
        return withTimeoutOrNull(AD_FREE_WAIT_MS) { repository.isAdFree.filterNotNull().first() } ?: false
    }
}
