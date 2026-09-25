package com.sirelon.marsroverphotos.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.Until

internal const val PACKAGE_NAME = "com.sirelon.marsroverphotos"

private const val UI_TIMEOUT_MS = 10_000L

// The first page comes over the network and composing it is slow while the app still runs
// uncompiled, which is exactly the state the generator starts from.
private const val PHOTOS_TIMEOUT_MS = 60_000L

private const val GRID_FLINGS = 5

private const val STALE_RETRIES = 3

// UiDevice.swipe steps take ~5 ms each, so this is a fast enough gesture to fling the grid.
private const val FLING_STEPS = 10

/**
 * Cold-starts the app and waits for the rover list, the start screen's first real content.
 *
 * Elements are found by Compose `testTag`, which `MainActivity` exposes as resource ids.
 */
internal fun MacrobenchmarkScope.startAndAwaitRovers() {
    pressHome()
    startActivityAndWait()
    check(device.wait(Until.hasObject(By.res("roverCard")), UI_TIMEOUT_MS)) {
        "Rover list did not appear"
    }
}

/** Opens Curiosity's photo feed and flings the grid down, loading further pages as it goes. */
internal fun MacrobenchmarkScope.scrollPhotoGrid() {
    // The rover list is ordered by mission data, so Curiosity can sit below the fold on a phone.
    retryOnStale {
        val roverList = checkNotNull(device.wait(Until.findObject(By.scrollable(true)), UI_TIMEOUT_MS)) {
            "Rover list is not scrollable"
        }
        checkNotNull(roverList.scrollUntil(Direction.DOWN, Until.findObject(By.text("Curiosity")))) {
            "Curiosity is not in the rover list"
        }.click()
    }
    // The grid is composed once the first page is in, and read once: afterwards it is driven by
    // coordinates, because while photos stream in the screen can report no accessibility root.
    val bounds = retryOnStale {
        checkNotNull(device.wait(Until.findObject(By.res("photoGrid")), PHOTOS_TIMEOUT_MS)) {
            "Photo grid did not load"
        }.visibleBounds
    }
    val x = bounds.centerX()
    val margin = bounds.height() / 5
    repeat(GRID_FLINGS) {
        device.swipe(x, bounds.bottom - margin, x, bounds.top + margin, FLING_STEPS)
        device.waitForIdle()
    }
}

/**
 * Runs [block] again when a node it holds is recomposed away mid-use. The rover list and the photo
 * grid both leave and re-enter composition while their data refreshes.
 */
private inline fun <T> retryOnStale(block: () -> T): T {
    repeat(STALE_RETRIES - 1) {
        try {
            return block()
        } catch (_: StaleObjectException) {
            // Look the node up again.
        }
    }
    return block()
}
