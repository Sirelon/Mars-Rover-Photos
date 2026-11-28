package com.sirelon.marsroverphotos.data.repositories

import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.data.network.RestApi
import com.sirelon.marsroverphotos.data.network.toMarsImages
import com.sirelon.marsroverphotos.data.paging.MER_KEYWORDS
import com.sirelon.marsroverphotos.data.paging.pageQuery
import com.sirelon.marsroverphotos.data.paging.usesPageFeed
import com.sirelon.marsroverphotos.data.phoenix.PhoenixCatalog
import com.sirelon.marsroverphotos.data.viking.VikingCatalog
import com.sirelon.marsroverphotos.domain.models.CURIOSITY_ID
import com.sirelon.marsroverphotos.domain.models.INGENUITY_ID
import com.sirelon.marsroverphotos.domain.models.OPPORTUNITY_ID
import com.sirelon.marsroverphotos.domain.models.PERSEVERANCE_ID
import com.sirelon.marsroverphotos.domain.models.PHOENIX_ID
import com.sirelon.marsroverphotos.domain.models.PhotosQueryRequest
import com.sirelon.marsroverphotos.domain.models.Rover
import com.sirelon.marsroverphotos.domain.models.VIKING_1_ID
import com.sirelon.marsroverphotos.domain.models.VIKING_2_ID
import com.sirelon.marsroverphotos.domain.models.mission.RoverMissionData
import com.sirelon.marsroverphotos.domain.repositories.PhotosRepository
import com.sirelon.marsroverphotos.utils.Logger
import kotlinx.coroutines.CancellationException
import kotlin.random.Random

/**
 * Implementation of PhotosRepository.
 *
 * Chooses where a rover's photos come from: the NASA raw-image APIs for every active or
 * recently-ended mission, and the bundled [VikingCatalog] and [PhoenixCatalog] for the Viking and
 * Phoenix landers, whose archives have no API to query.
 *
 * Created on 21.02.2021 20:19 for Mars-Rover-Photos.
 */
/** Opportunity's last mission year; its 2018 images are its own photos, top hit a self-portrait. */
private const val OPPORTUNITY_COVER_YEAR_START = 2018

/** Candidate frames whose pixels are checked for a raw-feed cover, newest first. */
private const val MAX_COVER_CHECKS = 6

/** images.nasa.gov results per page drawn for the Dig deck. */
private const val DIG_LIBRARY_PAGE_SIZE = 20

class PhotosRepositoryImpl(
    private val api: RestApi,
    private val vikingCatalog: VikingCatalog,
    private val phoenixCatalog: PhoenixCatalog = PhoenixCatalog(),
    /**
     * Judges a raw-feed candidate for a rover card ([CoverFrameCheck] in the app). Null takes the
     * newest candidate unchecked.
     */
    private val isGoodCover: (suspend (MarsImage) -> Boolean)? = null,
    private val random: Random = Random.Default,
) : PhotosRepository {

    /** images.nasa.gov result count per page-feed rover, learned on its first Dig draw. */
    private val libraryTotalHits = mutableMapOf<Long, Int>()

    override suspend fun refreshImages(query: PhotosQueryRequest): List<MarsImage> {
        return when (query.roverId) {
            VIKING_1_ID, VIKING_2_ID -> vikingCatalog.photosForSol(query.roverId, query.sol, query.camera)
            PHOENIX_ID -> phoenixCatalog.photosForSol(query.sol, query.camera)
            else -> api.getRoversPhotos(query)
        }
    }

    /**
     * Answered for Ingenuity only. Its 14,553 images sit on 162 of the 1,027 sols it spent on
     * Mars, so an unlucky random anchor can cost dozens of sequential probes before the feed
     * finds a photo; the feed's range operators collapse that to one request. Every other
     * mission is dense enough that the extra round-trip would cost more than it saves.
     */
    override suspend fun nearestSolWithPhotosAtOrAfter(roverId: Long, fromSol: Long): Long? {
        if (roverId != INGENUITY_ID) return null
        return runCatching { api.getIngenuityNearestSolAtOrAfter(fromSol) }.getOrNull()
    }

    /**
     * Per source: the active rovers (Perseverance, Curiosity) with their newest color-camera frame
     * that passes [isGoodCover] (complete and in color; at most [MAX_COVER_CHECKS] tried, none
     * passing means null); the closed InSight and Ingenuity missions with their final frame; the Viking
     * and Phoenix catalogues with the last frame of their last sol.
     * images.nasa.gov has no date order for Spirit and Opportunity, so they take the top hit of
     * the same search their photo feed runs. Opportunity's search starts at 2018: its unfiltered
     * top hit is an artist's concept, and a cover must be a real rover photo.
     */
    override suspend fun latestPhoto(roverId: Long): MarsImage? = when {
        roverId == VIKING_1_ID || roverId == VIKING_2_ID -> vikingCatalog.latestPhoto(roverId)
        roverId == PHOENIX_ID -> phoenixCatalog.latestPhoto()
        roverId.usesPageFeed() -> api
            .searchImages(
                roverId.pageQuery(),
                page = 1,
                pageSize = 1,
                keywords = MER_KEYWORDS,
                yearStart = if (roverId == OPPORTUNITY_ID) OPPORTUNITY_COVER_YEAR_START else null,
            )
            .toMarsImages(roverId)
            .firstOrNull()
        else -> latestGoodFullFrame(roverId)
    }

    /**
     * Where a random point of the mission comes from, per source:
     * - Perseverance, Curiosity, InSight: one random sol between the mission's first and its
     *   [Rover.maxSol]. Many sols hold no full frame, and the caller then draws again.
     * - Ingenuity: one random page of its closed archive. Its photos sit on 162 of 1,027 sols, so
     *   a random sol would miss six times out of seven. Every frame in that feed is a full frame.
     * - Viking and Phoenix: single frames from anywhere in the bundled catalogue.
     * - Spirit and Opportunity: one random page of the images.nasa.gov search their feed runs.
     *   The page count needs the result total, so the first draw per rover reads page 1 first.
     */
    override suspend fun randomPhotos(rover: Rover, count: Int): List<MarsImage> {
        val pool = when {
            rover.id == VIKING_1_ID || rover.id == VIKING_2_ID ->
                List(count) { vikingCatalog.randomPhoto(rover.id) }.filterNotNull()
            rover.id == PHOENIX_ID -> List(count) { phoenixCatalog.randomPhoto() }.filterNotNull()
            rover.id == INGENUITY_ID -> api.getIngenuityRandomPhoto()
            rover.id.usesPageFeed() -> randomLibraryPage(rover.id)
            else -> {
                val minSol = RoverMissionData.getMinSol(rover.id)
                val sol = random.nextLong(minSol, rover.maxSol.coerceAtLeast(minSol) + 1)
                api.getFullFramesForSol(rover.id, sol)
            }
        }
        return pool.distinctBy { it.id }.shuffled(random).take(count)
    }

    private suspend fun randomLibraryPage(roverId: Long): List<MarsImage> {
        suspend fun page(number: Int) = api.searchImages(
            roverId.pageQuery(),
            page = number,
            pageSize = DIG_LIBRARY_PAGE_SIZE,
            keywords = MER_KEYWORDS,
        )

        var firstPage: List<MarsImage>? = null
        val totalHits = libraryTotalHits[roverId] ?: page(1).let { response ->
            firstPage = response.toMarsImages(roverId)
            (response.collection.metadata?.totalHits ?: 0).also { libraryTotalHits[roverId] = it }
        }
        val pages = ((totalHits + DIG_LIBRARY_PAGE_SIZE - 1) / DIG_LIBRARY_PAGE_SIZE).coerceAtLeast(1)
        val target = random.nextInt(1, pages + 1)
        return firstPage.takeIf { target == 1 } ?: page(target).toMarsImages(roverId)
    }

    private suspend fun latestGoodFullFrame(roverId: Long): MarsImage? {
        val candidates = api.getLatestFullFrames(roverId)
        // Only the active rovers' candidates come from color cameras; the closed missions' final
        // frame is fixed (and Ingenuity's camera is greyscale), so it is taken as it is.
        val check = isGoodCover?.takeIf { roverId == PERSEVERANCE_ID || roverId == CURIOSITY_ID }
            ?: return candidates.firstOrNull()
        return candidates.take(MAX_COVER_CHECKS).firstOrNull { candidate ->
            try {
                check(candidate)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w("PhotosRepository") { "Cover check failed for ${candidate.imageUrl}: ${e.message}" }
                false
            }
        }
    }
}
