package com.sirelon.marsroverphotos.data.network

import com.sirelon.marsroverphotos.data.network.models.ReleaseDto
import com.sirelon.marsroverphotos.data.network.models.isActiveOn
import com.sirelon.marsroverphotos.platform.AppStore
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Each store's approval shows a release on that store's builds alone; `active` stays the fallback
 * for documents without per-store flags and for desktop.
 */
class ReleaseActiveFlagsTest {

    @Test
    fun playApprovalShowsTheReleaseOnAndroidOnly() {
        val release = ReleaseDto(version = "5.5.0", active = false, activeAndroid = true, activeIos = false)

        assertTrue(release.isActiveOn(AppStore.GOOGLE_PLAY))
        assertFalse(release.isActiveOn(AppStore.APP_STORE))
        assertFalse(release.isActiveOn(null))
    }

    @Test
    fun aStoreFlagOverridesActive() {
        val release = ReleaseDto(version = "5.5.0", active = true, activeIos = false)

        assertFalse(release.isActiveOn(AppStore.APP_STORE))
        assertTrue(release.isActiveOn(AppStore.GOOGLE_PLAY))
    }

    @Test
    fun withoutStoreFlagsEveryPlatformFollowsActive() {
        val live = ReleaseDto(version = "5.3.0", active = true)
        val pending = ReleaseDto(version = "5.4.0", active = false)

        for (store in listOf(AppStore.GOOGLE_PLAY, AppStore.APP_STORE, null)) {
            assertTrue(live.isActiveOn(store))
            assertFalse(pending.isActiveOn(store))
        }
    }
}
