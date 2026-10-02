package com.sirelon.marsroverphotos.domain.settings

import com.sirelon.marsroverphotos.platform.FakePreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppSettingsLaunchTest {

    @Test
    fun nothingIsRecordedBeforeTheFirstLaunch() {
        val settings = AppSettings(FakePreferences())
        assertEquals(0, settings.launchCount)
        assertNull(settings.firstLaunchAt)
        assertNull(settings.firstLaunchVersion)
    }

    @Test
    fun recordLaunchCountsEveryOpenAndPinsOnlyTheFirst() {
        val settings = AppSettings(FakePreferences())
        settings.recordLaunch(versionName = "5.1.0", nowMillis = 1_000L)
        settings.recordLaunch(versionName = "5.2.0", nowMillis = 2_000L)
        settings.recordLaunch(versionName = "5.3.0", nowMillis = 3_000L)

        assertEquals(3, settings.launchCount)
        assertEquals(1_000L, settings.firstLaunchAt)
        assertEquals("5.1.0", settings.firstLaunchVersion)
    }
}
