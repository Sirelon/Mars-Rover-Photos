package com.sirelon.marsroverphotos.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.domain.models.Rover
import com.sirelon.marsroverphotos.domain.repositories.ImagesRepository
import com.sirelon.marsroverphotos.domain.repositories.PhotosRepository
import com.sirelon.marsroverphotos.domain.repositories.RoversRepository
import com.sirelon.marsroverphotos.domain.settings.AppSettings
import com.sirelon.marsroverphotos.platform.Tracker
import com.sirelon.marsroverphotos.presentation.navigation.ScreenNames
import com.sirelon.marsroverphotos.utils.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * The deck shown on the Dig screen. [cards] holds the photos in order: the first is on top, the
 * rest wait under it so their images load before they come up. The deck is endless — it draws more
 * photos before it runs low — so [cards] is empty only while the first draw runs
 * ([isLoading]) or after every attempt failed ([loadFailed]).
 */
data class DigUiState(
    val cards: List<Card> = emptyList(),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val canUndo: Boolean = false,
) {
    /** One photo of the deck and the name of the mission that took it. */
    data class Card(val photo: MarsImage, val missionName: String)
}

/**
 * Dig: random raw photos from every mission, one at a time. Keep saves the photo to Favorites
 * through the same path as the heart elsewhere, so it also counts as a favorite in Firestore.
 * Skip only shows the next photo; it stores nothing. Neither touches the view counter, so random
 * frames never push a photo up Popular.
 *
 * Each draw picks a mission at random — never the one of the draw before, so the deck keeps
 * moving between missions — and asks [PhotosRepository.randomPhotos] for a few photos from one
 * random point of it, mixed in under the cards on screen. Photos already in Favorites and photos
 * shown earlier in this deck are dropped.
 */
class DigViewModel(
    private val photosRepository: PhotosRepository,
    private val imagesRepository: ImagesRepository,
    private val roversRepository: RoversRepository,
    private val appSettings: AppSettings,
    private val tracker: Tracker,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DigUiState())
    val uiState: StateFlow<DigUiState> = _uiState.asStateFlow()

    private val _keepNotices = Channel<Unit>(Channel.CONFLATED)

    /** Fires on the user's first Keep ever, so the screen can say where the photo went. */
    val keepNotices: Flow<Unit> = _keepNotices.receiveAsFlow()

    private val _openPhoto = Channel<String>(Channel.CONFLATED)

    /** The id of a photo to show in the full-screen viewer; it is in Room by the time it fires. */
    val openPhoto: Flow<String> = _openPhoto.receiveAsFlow()

    /**
     * A swiped card. For a Keep, [favoriteJob] writes the favorite and sets [favorited] only when
     * it changed something: the photo may already be a favorite, hearted in the full-screen viewer.
     */
    private class Swipe(val card: DigUiState.Card, val kept: Boolean) {
        var favoriteJob: Job? = null
        var favorited = false
    }

    private val queue = ArrayDeque<DigUiState.Card>()
    private val shownIds = mutableSetOf<String>()
    private var lastSwipe: Swipe? = null
    private var lastRoverId: Long? = null
    private var drawJob: Job? = null

    init {
        drawIfLow()
    }

    fun keep() {
        val card = queue.removeFirstOrNull() ?: return
        val swipe = Swipe(card, kept = true)
        lastSwipe = swipe
        swipe.favoriteJob = viewModelScope.launch {
            val stored = storedPhoto(card.photo.id)
            if (stored?.favorite == true) return@launch
            imagesRepository.setFavorite(stored ?: card.photo, true)
            swipe.favorited = true
        }
        tracker.trackFavorite(card.photo, from = ScreenNames.DIG, fav = true)
        trackSwipe(card, ACTION_KEEP)
        if (!appSettings.digKeepNoticeShown) {
            appSettings.digKeepNoticeShown = true
            _keepNotices.trySend(Unit)
        }
        publish()
        drawIfLow()
    }

    fun skip() {
        val card = queue.removeFirstOrNull() ?: return
        lastSwipe = Swipe(card, kept = false)
        trackSwipe(card, ACTION_SKIP)
        publish()
        drawIfLow()
    }

    /** Puts the last swiped photo back on top. A kept photo leaves Favorites again. */
    fun undo() {
        val swipe = lastSwipe ?: return
        lastSwipe = null
        queue.addFirst(swipe.card)
        if (swipe.kept) {
            val photo = swipe.card.photo
            viewModelScope.launch {
                swipe.favoriteJob?.join()
                if (!swipe.favorited) return@launch
                // The Room row carries the counter the Keep raised, which setFavorite counts down from.
                imagesRepository.setFavorite(storedPhoto(photo.id) ?: photo, false)
            }
            tracker.trackFavorite(photo, from = ScreenNames.DIG, fav = false)
        }
        trackSwipe(swipe.card, ACTION_UNDO)
        publish()
    }

    /**
     * Opens [card]'s photo in the full-screen viewer. The viewer reads photos from Room, and a
     * deck photo is not there until it is saved.
     */
    fun open(card: DigUiState.Card) {
        viewModelScope.launch {
            imagesRepository.saveImages(listOf(card.photo))
            _openPhoto.send(card.photo.id)
        }
    }

    private suspend fun storedPhoto(id: String): MarsImage? =
        imagesRepository.loadImages(listOf(id)).first().firstOrNull()

    /** Starts over after every draw failed. */
    fun retry() {
        _uiState.value = _uiState.value.copy(isLoading = true, loadFailed = false)
        drawIfLow()
    }

    private fun drawIfLow() {
        if (queue.size >= LOW_WATER || drawJob?.isActive == true) return
        drawJob = viewModelScope.launch { draw() }
    }

    /** Draws until the deck holds [FULL] photos, or [MAX_FRUITLESS_DRAWS] draws in a row add none. */
    private suspend fun draw() {
        val rovers = roversRepository.getRovers().first { it.isNotEmpty() }
        var fruitless = 0
        while (queue.size < FULL && fruitless < MAX_FRUITLESS_DRAWS) {
            val rover = pickRover(rovers)
            val added = try {
                fresh(photosRepository.randomPhotos(rover, BATCH_SIZE))
                    .map { DigUiState.Card(it, rover.name) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e("DigViewModel", e) { "Drawing photos of ${rover.name} failed" }
                emptyList()
            }
            if (added.isEmpty()) {
                fruitless++
                continue
            }
            fruitless = 0
            mixIn(added)
            shownIds += added.map { it.photo.id }
            publish()
        }
        _uiState.value = _uiState.value.copy(isLoading = false, loadFailed = queue.isEmpty())
    }

    /**
     * Adds a draw's photos and shuffles everything under the visible cards. One draw comes from
     * one sol, so without the shuffle its photos would follow each other; the cards on screen
     * stay where they are.
     */
    private fun mixIn(cards: List<DigUiState.Card>) {
        queue.addAll(cards)
        if (queue.size <= VISIBLE_CARDS + 1) return
        val hidden = queue.drop(VISIBLE_CARDS).shuffled()
        while (queue.size > VISIBLE_CARDS) queue.removeLast()
        queue.addAll(hidden)
    }

    private fun pickRover(rovers: List<Rover>): Rover {
        val rover = rovers.filter { it.id != lastRoverId }.ifEmpty { rovers }.random()
        lastRoverId = rover.id
        return rover
    }

    /**
     * Drops photos already shown in this deck or already in Favorites, and carries over the
     * counters Room holds for the rest, so a Keep adds to the real favorite count.
     */
    private suspend fun fresh(photos: List<MarsImage>): List<MarsImage> {
        val unseen = photos.filter { it.id !in shownIds }
        if (unseen.isEmpty()) return unseen
        val stored = imagesRepository.loadImages(unseen.map { it.id }).first().associateBy { it.id }
        return unseen.mapNotNull { photo ->
            val row = stored[photo.id] ?: return@mapNotNull photo
            if (row.favorite) null else photo.copy(stats = row.stats)
        }
    }

    private fun publish() {
        _uiState.value = _uiState.value.copy(
            cards = queue.take(VISIBLE_CARDS),
            isLoading = queue.isEmpty() && drawJob?.isActive == true,
            loadFailed = false,
            canUndo = lastSwipe != null,
        )
    }

    private fun trackSwipe(card: DigUiState.Card, action: String) {
        tracker.trackEvent(
            EVENT_SWIPE,
            mapOf("action" to action, "rover_id" to card.photo.roverId.toString()),
        )
    }

    internal companion object {
        /** Photos taken from one random point of a mission; few, so the deck mixes missions. */
        const val BATCH_SIZE = 4

        /** The deck draws again when fewer photos than this are left. */
        const val LOW_WATER = 4

        /** A draw stops once the deck holds this many photos. */
        const val FULL = 12

        /** Draws in a row that add no photo before the deck reports a failure. */
        const val MAX_FRUITLESS_DRAWS = 8

        /** The top card and two under it, so quick swipes still land on a loaded photo. */
        const val VISIBLE_CARDS = 3

        const val EVENT_SWIPE = "dig_swipe"
        const val ACTION_KEEP = "keep"
        const val ACTION_SKIP = "skip"
        const val ACTION_UNDO = "undo"
    }
}
