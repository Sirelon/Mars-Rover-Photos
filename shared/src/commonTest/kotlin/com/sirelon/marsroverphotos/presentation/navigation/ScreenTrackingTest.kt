package com.sirelon.marsroverphotos.presentation.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScreenTrackingTest {

    private val allDestinations = listOf(
        AppDestination.Rovers,
        AppDestination.Favorite,
        AppDestination.Popular,
        AppDestination.About,
        AppDestination.Ukraine,
        AppDestination.AdminPhotos,
        AppDestination.Photos(roverId = 5),
        AppDestination.Mission(roverId = 5),
        AppDestination.PhotosDateJumpPicker(roverId = 5),
        AppDestination.PhotosFilters(roverId = 5),
        AppDestination.Images(),
    )

    @Test
    fun `every destination maps to a distinct snake_case screen name`() {
        val names = allDestinations.map { it.toScreenView().name }
        assertEquals(names.size, names.toSet().size, "duplicate screen names: $names")
        names.forEach {
            assertTrue(it.matches(Regex("[a-z][a-z0-9_]*")), "not snake_case: $it")
        }
    }

    @Test
    fun `rover-scoped destinations carry rover_id`() {
        assertEquals(
            mapOf("rover_id" to "5"),
            AppDestination.Photos(roverId = 5).toScreenView().params,
        )
        assertEquals(
            mapOf("rover_id" to "5"),
            AppDestination.Mission(roverId = 5).toScreenView().params,
        )
    }

    @Test
    fun `images carries its source and only carries rover_id when feed-backed`() {
        assertEquals(
            ScreenView("photo_detail", mapOf("source" to "direct_ids")),
            AppDestination.Images(photoIds = listOf("a"), selectedId = "a").toScreenView(),
        )
        assertEquals(
            ScreenView("photo_detail", mapOf("source" to "rover_feed", "rover_id" to "7")),
            AppDestination.Images(
                selectedId = "a",
                source = AppDestination.ImagesSource.ROVER_FEED,
                roverId = 7,
            ).toScreenView(),
        )
    }

    @Test
    fun `changing a photos camera filter does not count as a new screen view`() {
        assertEquals(
            AppDestination.Photos(roverId = 5).toScreenView(),
            AppDestination.Photos(roverId = 5, camera = "NAVCAM").toScreenView(),
        )
    }

    @Test
    fun `swiping to another photo does not count as a new screen view`() {
        val feed = { id: String ->
            AppDestination.Images(
                selectedId = id,
                source = AppDestination.ImagesSource.ROVER_FEED,
                roverId = 7,
            ).toScreenView()
        }
        assertEquals(feed("a"), feed("b"))
    }
}
