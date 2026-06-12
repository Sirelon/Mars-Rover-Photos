package com.sirelon.marsroverphotos.data.network

import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.data.network.models.NasaImagesSearchResponse
import com.sirelon.marsroverphotos.domain.models.CURIOSITY_ID
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

/**
 * @author romanishin
 * @since 31.10.16 on 15:28
 * Main REST API facade for fetching Mars rover photos and data.
 */
class RestApi {

    private val _perseveranceTotalImages = MutableStateFlow<Long?>(null)
    val perseveranceTotalImages = _perseveranceTotalImages.filterNotNull().distinctUntilChanged()

    private val _insightTotalImages = MutableStateFlow<Long?>(null)
    val insightTotalImages = _insightTotalImages.filterNotNull().distinctUntilChanged()

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
     * @throws IllegalArgumentException if the rover ID is not supported.
     *         Supported rover IDs: PERSEVERANCE_ID, INSIGHT_ID, CURIOSITY_ID, OPPORTUNITY_ID, SPIRIT_ID
     */
    suspend fun getRoversPhotos(query: PhotosQueryRequest): List<MarsImage> {
        // We should call another api if rover is insight or perseverance
        val sol = query.sol
        return when (query.roverId) {
            PERSEVERANCE_ID -> {
                loadPerseverance(query)
            }

            INSIGHT_ID -> {
                val response = nasaApi.getRawImages("insight", from = "$sol:sol", to = "$sol:sol")
                response.total?.let { _insightTotalImages.value = it }
                response.list.mapToUi()
            }

            CURIOSITY_ID -> {
                nasaApi.getRawImages("msl", from = "$sol:sol", to = "$sol:sol")
                    .list.mapToUiMsl()
            }

            else -> throw IllegalArgumentException("Unsupported rover id: ${query.roverId}")
        }
    }

    private suspend fun loadPerseverance(query: PhotosQueryRequest): List<MarsImage> {
        val response = nasaApi.getPerseveranceRawImages(sol = "${query.sol}:sol:in")
        _perseveranceTotalImages.value = response.totalImages
        return response.photos.preveranceToUI()
    }

    suspend fun getInsightLatestPhotos(): List<MarsImage> {
        return nasaApi.getRawImages("insight").list.mapToUi()
    }

    suspend fun getCuriosityLatestPhotos(): List<MarsImage> {
        return nasaApi.getRawImages("msl").list.mapToUiMsl()
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
        return response.photos.preveranceToUI()
    }
}
