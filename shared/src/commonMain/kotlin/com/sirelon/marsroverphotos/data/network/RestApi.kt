package com.sirelon.marsroverphotos.data.network

import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.data.network.models.NasaImagesSearchResponse
import com.sirelon.marsroverphotos.domain.models.CURIOSITY_ID
import com.sirelon.marsroverphotos.domain.models.INGENUITY_ID
import com.sirelon.marsroverphotos.domain.models.INSIGHT_ID
import com.sirelon.marsroverphotos.domain.models.PERSEVERANCE_ID
import com.sirelon.marsroverphotos.domain.models.PhotosQueryRequest
import com.sirelon.marsroverphotos.platform.createHttpClientEngine
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlin.random.Random

/** Pages of Ingenuity's closed archive: 14,553 images at the feed's cap of 100 per page. */
private const val INGENUITY_FEED_PAGES = 146

/**
 * @author romanishin
 * @since 31.10.16 on 15:28
 * Main REST API facade for fetching Mars rover photos and data.
 */
class RestApi {

    private val _perseveranceTotalImages = MutableStateFlow<Long?>(null)

    /**
     * Mission-wide Perseverance photo count.
     *
     * Only fed by UNFILTERED requests. The raw-image feeds return `total_images`/`total` for the
     * *current query*, so a sol- or camera-filtered request reports that slice's count — publishing
     * it here would overwrite the rover's mission total with a per-sol number and make the
     * "Total Photos" stat flip on every page load (SIR-80).
     */
    val perseveranceTotalImages = _perseveranceTotalImages.filterNotNull().distinctUntilChanged()

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val ktor = HttpClient(createHttpClientEngine()) {
        // default validation to throw exceptions for non-2xx responses
        expectSuccess = true

        // set default request parameters
        defaultRequest {
            // add base url for all request
            url("https://api.nasa.gov")
        }

        install(ContentNegotiation) {
            json(json)
        }

        install(HttpTimeout) {
            val timeout = 120_000L // 2 minutes in milliseconds
            connectTimeoutMillis = timeout
            requestTimeoutMillis = timeout
            socketTimeoutMillis = timeout
        }
    }

    private val nasaApi: NasaApi = NasaApi(ktor)

    /**
     * Fetch rover photos from NASA API for a specific query.
     *
     * @param query The photos query request containing rover ID, sol, and camera
     * @return List of Mars images for the specified query
     * @throws IllegalArgumentException if the rover ID has no raw-image endpoint here.
     *         Supported rover IDs: PERSEVERANCE_ID, INGENUITY_ID, INSIGHT_ID, CURIOSITY_ID.
     *         Spirit and Opportunity are served by [searchImages], and the Viking landers by
     *         `VikingCatalog`; both are routed before this call in `PhotosRepositoryImpl`.
     */
    suspend fun getRoversPhotos(query: PhotosQueryRequest): List<MarsImage> {
        // We should call another api if rover is insight or perseverance
        val sol = query.sol
        return when (query.roverId) {
            PERSEVERANCE_ID -> {
                loadSolFeed(query, category = "mars2020")
            }

            INGENUITY_ID -> {
                loadSolFeed(query, category = "ingenuity")
            }

            INSIGHT_ID -> {
                // `response.total` is the count for THIS sol only — not the mission total.
                nasaApi.getRawImages("insight", from = "$sol:sol", to = "$sol:sol")
                    .list.mapToUi(query.roverId)
            }

            CURIOSITY_ID -> {
                nasaApi.getRawImages("msl", from = "$sol:sol", to = "$sol:sol")
                    .list.mapToUiMsl(query.roverId)
            }

            else -> throw IllegalArgumentException("Unsupported rover id: ${query.roverId}")
        }
    }

    /**
     * One sol of the Mars 2020 raw-image feed, for whichever mission [category] selects
     * (`mars2020` or `ingenuity`). Both share the feed, the record shape and the mapper.
     */
    private suspend fun loadSolFeed(
        query: PhotosQueryRequest,
        category: String,
    ): List<MarsImage> {
        // `response.totalImages` counts only the photos matching this sol/camera query, so it is
        // deliberately NOT published to [perseveranceTotalImages].
        val response = nasaApi.getPerseveranceRawImages(
            sol = "${query.sol}:sol:in",
            category = category,
        )
        return response.photos.preveranceToUI(query.roverId)
    }

    /**
     * A random Ingenuity frame, for the widget.
     *
     * Ingenuity's campaign closed in 2024, so there is no "latest" to track — a latest-photo
     * widget would show one frozen image forever. Like the Viking landers, it draws from the
     * whole archive instead: 14,553 images at the feed's hard cap of 100 per page means pages
     * 0..145 carry data.
     *
     * Deliberately does not go through [getPerseveranceLatestPhotos] and never writes to
     * [_perseveranceTotalImages]: this response's `total_images` is Ingenuity's 14,553, and
     * publishing it there would overwrite Perseverance's "Total Photos" stat (SIR-80).
     */
    suspend fun getIngenuityRandomPhoto(): List<MarsImage> {
        val response = nasaApi.getPerseveranceRawImages(
            category = "ingenuity",
            page = Random.nextInt(0, INGENUITY_FEED_PAGES),
        )
        return response.photos.preveranceToUI(INGENUITY_ID)
    }

    /**
     * First sol at or after [fromSol] holding an Ingenuity photo, or null when the lookup fails
     * or there is none. Used to pick an opening anchor on Ingenuity's sparse feed.
     */
    suspend fun getIngenuityNearestSolAtOrAfter(fromSol: Long): Long? =
        nasaApi.getIngenuityNearestSolAtOrAfter(fromSol)

    suspend fun getInsightLatestPhotos(): List<MarsImage> {
        return nasaApi.getRawImages("insight").list.mapToUi(INSIGHT_ID)
    }

    suspend fun getCuriosityLatestPhotos(): List<MarsImage> {
        return nasaApi.getRawImages("msl").list.mapToUiMsl(CURIOSITY_ID)
    }

    /** Returns the latest Curiosity sol from the MSL raw feed (feed is ordered newest-first). */
    suspend fun getCuriosityLatestSol(): Long? {
        return nasaApi.getRawImages("msl").list.firstOrNull()?.sol
    }

    /** Fetches one page of images.nasa.gov search results (1-based page index). */
    suspend fun searchImages(
        query: String,
        page: Int,
        pageSize: Int = 100,
        keywords: String? = null,
    ): NasaImagesSearchResponse = nasaApi.searchImages(query, page, pageSize, keywords)

    suspend fun getPerseveranceLatestPhotos(count: Int = 1): List<MarsImage> {
        val response = nasaApi.getPerseveranceRawImages(count = count)
        _perseveranceTotalImages.value = response.totalImages
        return response.photos.preveranceToUI(PERSEVERANCE_ID)
    }
}
