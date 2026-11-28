package com.sirelon.marsroverphotos.presentation.viewmodels

import androidx.paging.PagingData
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.domain.models.PhotosQueryRequest
import com.sirelon.marsroverphotos.domain.models.Rover
import com.sirelon.marsroverphotos.domain.repositories.FavoriteCounts
import com.sirelon.marsroverphotos.domain.repositories.FavoriteSortOrder
import com.sirelon.marsroverphotos.domain.repositories.ImagesRepository
import com.sirelon.marsroverphotos.domain.repositories.PhotosRepository
import com.sirelon.marsroverphotos.domain.repositories.RoverCount
import com.sirelon.marsroverphotos.domain.repositories.RoversRepository
import com.sirelon.marsroverphotos.domain.settings.AppSettings
import com.sirelon.marsroverphotos.platform.FakePreferences
import com.sirelon.marsroverphotos.platform.RecordingTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

private fun rover(id: Long, name: String) = Rover(
    id = id,
    name = name,
    drawableName = "",
    landingDate = "",
    launchDate = "",
    status = "active",
    maxSol = 100L,
    maxDate = "",
    totalPhotos = 0,
)

private fun photo(id: String, roverId: Long, favoriteCount: Long = 0) = MarsImage(
    id = id,
    order = 0,
    sol = 1,
    name = null,
    imageUrl = "https://example.com/$id.jpg",
    earthDate = "2026-01-01",
    roverId = roverId,
    camera = null,
    stats = MarsImage.Stats(see = 0, scale = 0, save = 0, share = 0, favorite = favoriteCount),
)

private class FakeRovers(private val rovers: List<Rover>) : RoversRepository {
    override fun initialize() = Unit
    override fun getRovers(): Flow<List<Rover>> = flowOf(rovers)
    override suspend fun loadRoverById(id: Long): Rover? = rovers.firstOrNull { it.id == id }
    override suspend fun updateRoverCountPhotos(roverId: Long, photos: Long) = Unit
}

/** Hands out new photos on every draw, numbered per rover, and records which rover each draw asked for. */
private class FakeRandomPhotos(var failing: Boolean = false) : PhotosRepository {
    val drawnRovers = mutableListOf<Long>()
    private var next = 0

    override suspend fun refreshImages(query: PhotosQueryRequest): List<MarsImage> = emptyList()

    override suspend fun randomPhotos(rover: Rover, count: Int): List<MarsImage> {
        drawnRovers += rover.id
        if (failing) error("offline")
        return List(count) { photo("p${next++}", rover.id) }
    }
}

/** Records [setFavorite] calls; [stored] plays the Room rows [loadImages] finds. */
private class FakeImages(private val stored: Map<String, MarsImage> = emptyMap()) : ImagesRepository {
    val favoriteCalls = mutableListOf<Pair<MarsImage, Boolean>>()

    override suspend fun setFavorite(item: MarsImage, favorite: Boolean) {
        favoriteCalls += item to favorite
    }

    override fun loadImages(ids: List<String>): Flow<List<MarsImage>> = flowOf(ids.mapNotNull { stored[it] })

    override suspend fun saveImages(photos: List<MarsImage>) = Unit
    override fun loadFavoriteImages(): Flow<List<MarsImage>> = emptyFlow()
    override fun loadPopularImages(): Flow<List<MarsImage>> = emptyFlow()
    override fun loadFavoritePaged(sort: FavoriteSortOrder, roverId: Long?): Flow<PagingData<MarsImage>> = emptyFlow()
    override fun loadFavoriteRoverCounts(): Flow<List<RoverCount>> = emptyFlow()
    override fun loadFavoriteCounts(): Flow<FavoriteCounts> = emptyFlow()
    override suspend fun loadFavoriteIndex(targetId: String, sort: FavoriteSortOrder, roverId: Long?): Int = 0
    override suspend fun updateFavForImage(item: MarsImage) = Unit
    override fun loadPopularPagedSource(): Flow<PagingData<MarsImage>> = emptyFlow()
    override suspend fun loadPopularPage(count: Int, after: MarsImage?): List<MarsImage> = emptyList()
}

@OptIn(ExperimentalCoroutinesApi::class)
class DigViewModelTest {

    private val curiosity = rover(5, "Curiosity")
    private val spirit = rover(7, "Spirit")

    private val photos = FakeRandomPhotos()
    private val tracker = RecordingTracker()
    private val settings = AppSettings(FakePreferences())

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(
        images: FakeImages = FakeImages(),
        rovers: List<Rover> = listOf(curiosity, spirit),
    ): DigViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return DigViewModel(photos, images, FakeRovers(rovers), settings, tracker)
    }

    @Test
    fun opensWithPhotosOnTheDeck() = runTest {
        val viewModel = viewModel()
        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(DigViewModel.VISIBLE_CARDS, state.cards.size)
        assertFalse(state.canUndo)
    }

    @Test
    fun drawsMoveBetweenMissions() = runTest {
        viewModel()
        advanceUntilIdle()

        assertTrue(photos.drawnRovers.size > 1)
        photos.drawnRovers.zipWithNext().forEach { (a, b) -> assertNotEquals(a, b) }
    }

    @Test
    fun keepAddsTheTopPhotoToFavoritesAndShowsTheNext() = runTest {
        val images = FakeImages()
        val viewModel = viewModel(images)
        advanceUntilIdle()
        val (top, under) = viewModel.uiState.value.cards

        viewModel.keep()
        advanceUntilIdle()

        assertEquals(listOf(top.photo to true), images.favoriteCalls)
        assertEquals(under, viewModel.uiState.value.cards.first())
        assertTrue(viewModel.uiState.value.canUndo)
        assertEquals("keep", tracker.named(DigViewModel.EVENT_SWIPE).single().params["action"])
    }

    @Test
    fun skipStoresNothing() = runTest {
        val images = FakeImages()
        val viewModel = viewModel(images)
        advanceUntilIdle()
        val under = viewModel.uiState.value.cards[1]

        viewModel.skip()
        advanceUntilIdle()

        assertTrue(images.favoriteCalls.isEmpty())
        assertEquals(under, viewModel.uiState.value.cards.first())
        assertEquals("skip", tracker.named(DigViewModel.EVENT_SWIPE).single().params["action"])
    }

    @Test
    fun theSavedNoticeFiresOnTheFirstKeepOnly() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        val notices = mutableListOf<Unit>()
        backgroundScope.launch { viewModel.keepNotices.collect { notices += it } }

        viewModel.keep()
        advanceUntilIdle()
        viewModel.keep()
        advanceUntilIdle()

        assertEquals(1, notices.size)
        assertTrue(settings.digKeepNoticeShown)
    }

    @Test
    fun theSavedNoticeStaysQuietAfterAnEarlierSession() = runTest {
        settings.digKeepNoticeShown = true
        val viewModel = viewModel()
        advanceUntilIdle()
        val notices = mutableListOf<Unit>()
        backgroundScope.launch { viewModel.keepNotices.collect { notices += it } }

        viewModel.keep()
        advanceUntilIdle()

        assertTrue(notices.isEmpty())
    }

    @Test
    fun undoingAKeepPutsThePhotoBackAndTakesItOutOfFavorites() = runTest {
        val images = FakeImages()
        val viewModel = viewModel(images)
        advanceUntilIdle()
        val top = viewModel.uiState.value.cards.first()

        viewModel.keep()
        advanceUntilIdle()
        viewModel.undo()
        advanceUntilIdle()

        assertEquals(top, viewModel.uiState.value.cards.first())
        assertFalse(viewModel.uiState.value.canUndo)
        val (undone, favorite) = images.favoriteCalls.last()
        assertFalse(favorite)
        assertEquals(top.photo.id, undone.id)
        // setFavorite derives the new count from the photo it gets: one more than before the Keep,
        // so the decrement lands back on the original count.
        assertEquals(top.photo.stats.favorite + 1, undone.stats.favorite)
    }

    @Test
    fun undoingASkipStoresNothing() = runTest {
        val images = FakeImages()
        val viewModel = viewModel(images)
        advanceUntilIdle()
        val top = viewModel.uiState.value.cards.first()

        viewModel.skip()
        viewModel.undo()
        advanceUntilIdle()

        assertEquals(top, viewModel.uiState.value.cards.first())
        assertTrue(images.favoriteCalls.isEmpty())
    }

    @Test
    fun onlyTheLastSwipeCanBeUndone() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        val (first, second) = viewModel.uiState.value.cards

        viewModel.skip()
        viewModel.skip()
        viewModel.undo()
        viewModel.undo()
        advanceUntilIdle()

        assertEquals(second, viewModel.uiState.value.cards.first())
        assertNotEquals(first, viewModel.uiState.value.cards.first())
    }

    @Test
    fun photosAlreadyInFavoritesAreLeftOut() = runTest {
        // The fake numbers photos p0, p1, …: make the first draw's photos favorites already.
        val stored = (0 until DigViewModel.BATCH_SIZE).associate { i ->
            "p$i" to photo("p$i", curiosity.id).copy(favorite = true)
        }
        val viewModel = viewModel(FakeImages(stored))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.cards.none { it.photo.id in stored })
    }

    @Test
    fun aKeepCountsFromTheFavoriteCountRoomAlreadyHolds() = runTest {
        val images = FakeImages(mapOf("p0" to photo("p0", curiosity.id, favoriteCount = 7)))
        val viewModel = viewModel(images, rovers = listOf(curiosity))
        advanceUntilIdle()
        val top = viewModel.uiState.value.cards.first()

        viewModel.keep()
        advanceUntilIdle()

        assertEquals("p0", top.photo.id)
        assertEquals(7, images.favoriteCalls.single().first.stats.favorite)
    }

    @Test
    fun theDeckNeverRunsOut() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        repeat(DigViewModel.FULL * 3) {
            viewModel.skip()
            advanceUntilIdle()
        }

        assertEquals(DigViewModel.VISIBLE_CARDS, viewModel.uiState.value.cards.size)
    }

    @Test
    fun failingDrawsEndInAFailureThatRetryRecovers() = runTest {
        photos.failing = true
        val viewModel = viewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.loadFailed)
        assertTrue(viewModel.uiState.value.cards.isEmpty())
        assertEquals(DigViewModel.MAX_FRUITLESS_DRAWS, photos.drawnRovers.size)

        photos.failing = false
        viewModel.retry()
        assertTrue(viewModel.uiState.value.isLoading)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.loadFailed)
        assertEquals(DigViewModel.VISIBLE_CARDS, viewModel.uiState.value.cards.size)
    }

    @Test
    fun cardsNameTheirMission() = runTest {
        val viewModel = viewModel(rovers = listOf(spirit))
        advanceUntilIdle()

        assertEquals("Spirit", viewModel.uiState.value.cards.first().missionName)
        assertEquals("Spirit", viewModel.uiState.value.cards.map { it.missionName }.distinct().single())
        assertEquals(spirit.id, photos.drawnRovers.first())
    }
}
