package com.sirelon.marsroverphotos.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json

/**
 * [navBackStackConfiguration] hand-registers every [AppDestination] subtype as a polymorphic
 * [NavKey] — required because [NavKey] is an external, non-sealed interface, so Kotlin's automatic
 * sealed-hierarchy polymorphism doesn't cover it. Nothing else keeps that list in sync with
 * [AppDestination] as destinations are added, so a forgotten subtype fails silently at runtime
 * (that destination just doesn't survive process death). This round-trips one instance of every
 * subtype through the same [kotlinx.serialization.modules.SerializersModule] the real back stack
 * uses, so a missing registration throws here instead.
 */
class AppDestinationSerializationTest {

    private val allDestinations: List<AppDestination> = listOf(
        AppDestination.Rovers,
        AppDestination.Photos(roverId = 5),
        AppDestination.Images(),
        AppDestination.Favorite,
        AppDestination.Popular,
        AppDestination.Mission(roverId = 5),
        AppDestination.About,
        AppDestination.Ukraine,
        AppDestination.AdminPhotos,
        AppDestination.PhotosDateJumpPicker(roverId = 5),
        AppDestination.PhotosFilters(roverId = 5),
        AppDestination.WhatsNewDialog,
        AppDestination.AllVersions,
        AppDestination.WhatsNewStory(version = "5.0.0"),
    )

    private val json = Json { serializersModule = navBackStackConfiguration.serializersModule }

    @Test
    fun `every AppDestination subtype round-trips as a polymorphic NavKey`() {
        allDestinations.forEach { destination ->
            val encoded = json.encodeToString(PolymorphicSerializer(NavKey::class), destination)
            val decoded = json.decodeFromString(PolymorphicSerializer(NavKey::class), encoded)
            assertEquals(destination, decoded, "round-trip failed for ${destination::class.simpleName}")
        }
    }
}
