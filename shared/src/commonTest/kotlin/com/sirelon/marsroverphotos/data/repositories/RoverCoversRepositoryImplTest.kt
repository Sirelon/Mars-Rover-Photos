package com.sirelon.marsroverphotos.data.repositories

import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.domain.models.PhotosQueryRequest
import com.sirelon.marsroverphotos.domain.repositories.PhotosRepository
import com.sirelon.marsroverphotos.platform.FakePreferences
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private const val ROVER_A = 1L
private const val ROVER_B = 2L

private fun photo(roverId: Long, url: String) = MarsImage(
    id = url,
    order = 0,
    sol = 0,
    name = null,
    imageUrl = url,
    earthDate = "",
    roverId = roverId,
    camera = null,
    stats = MarsImage.Stats(0, 0, 0, 0, 0),
)

/** Answers [PhotosRepository.latestPhoto] from [answers]; a missing rover throws. */
private class FakeLatestPhotos(
    private val answers: Map<Long, suspend () -> MarsImage?>,
) : PhotosRepository {
    val calls = mutableListOf<Long>()

    override suspend fun refreshImages(query: PhotosQueryRequest): List<MarsImage> = emptyList()

    override suspend fun latestPhoto(roverId: Long): MarsImage? {
        calls += roverId
        return answers[roverId]?.invoke() ?: error("offline")
    }
}

class RoverCoversRepositoryImplTest {

    private fun TestScope.repository(
        photos: PhotosRepository,
        preferences: FakePreferences = FakePreferences(),
    ) = RoverCoversRepositoryImpl(
        photosRepository = photos,
        preferences = preferences,
        appScope = backgroundScope,
        roverIds = listOf(ROVER_A, ROVER_B),
    )

    @Test
    fun aColdStartShowsTheSavedPhotosBeforeAnyRequest() = runTest {
        val preferences = FakePreferences()
        repository(FakeLatestPhotos(mapOf(ROVER_A to { photo(ROVER_A, "new-a") })), preferences)
            .apply { refreshOnce() }
        runCurrent()

        // Next launch: the saved URL is there before refreshOnce() runs.
        val photos = FakeLatestPhotos(emptyMap())
        val nextLaunch = repository(photos, preferences)

        assertEquals(mapOf(ROVER_A to "new-a"), nextLaunch.coverUrls.value)
        assertEquals(emptyList<Long>(), photos.calls)
    }

    @Test
    fun refreshesOncePerProcessHoweverOftenItIsAsked() = runTest {
        val photos = FakeLatestPhotos(
            mapOf(ROVER_A to { photo(ROVER_A, "a") }, ROVER_B to { photo(ROVER_B, "b") })
        )
        val repository = repository(photos)

        repository.refreshOnce()
        repository.refreshOnce()
        runCurrent()
        repository.refreshOnce()
        runCurrent()

        assertEquals(listOf(ROVER_A, ROVER_B), photos.calls.sorted())
        assertEquals(mapOf(ROVER_A to "a", ROVER_B to "b"), repository.coverUrls.value)
    }

    @Test
    fun aFailedFetchKeepsTheSavedPhoto() = runTest {
        val preferences = FakePreferences().apply { setString("roverCoverUrl_$ROVER_B", "saved-b") }
        val repository = repository(
            FakeLatestPhotos(mapOf(ROVER_A to { photo(ROVER_A, "a") })),
            preferences,
        )

        repository.refreshOnce()
        runCurrent()

        assertEquals(mapOf(ROVER_A to "a", ROVER_B to "saved-b"), repository.coverUrls.value)
    }

    @Test
    fun aSlowRoverDoesNotHoldBackTheOthers() = runTest {
        val slow = CompletableDeferred<MarsImage?>()
        val repository = repository(
            FakeLatestPhotos(
                mapOf(ROVER_A to { slow.await() }, ROVER_B to { photo(ROVER_B, "b") })
            )
        )

        repository.refreshOnce()
        runCurrent()
        assertEquals(mapOf(ROVER_B to "b"), repository.coverUrls.value)

        slow.complete(photo(ROVER_A, "a"))
        runCurrent()
        assertEquals(mapOf(ROVER_A to "a", ROVER_B to "b"), repository.coverUrls.value)
    }
}
