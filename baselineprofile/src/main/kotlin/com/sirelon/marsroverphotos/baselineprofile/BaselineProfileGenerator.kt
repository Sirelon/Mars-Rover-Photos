package com.sirelon.marsroverphotos.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records the classes and methods the app runs on its critical journeys into the Baseline Profile
 * that ships with the release build, so ART compiles them ahead of time instead of interpreting
 * them on first launch.
 *
 * Regenerate after meaningful startup or photo-grid changes:
 * `./gradlew :androidApp:generateBaselineProfile`. The output lands in
 * `androidApp/src/release/generated/baselineProfiles/` and is committed.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startup() = rule.collect(
        packageName = PACKAGE_NAME,
        // Also emit a startup profile, which R8 uses to lay out startup code in the primary dex.
        // Only this journey feeds it: the smaller it is, the more that layout helps.
        includeInStartupProfile = true,
    ) {
        startAndAwaitRovers()
    }

    @Test
    fun photoGrid() = rule.collect(packageName = PACKAGE_NAME) {
        startAndAwaitRovers()
        scrollPhotoGrid()
    }
}
