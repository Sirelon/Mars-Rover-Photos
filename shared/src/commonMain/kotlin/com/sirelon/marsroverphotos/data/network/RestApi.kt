package com.sirelon.marsroverphotos.data.network

import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.data.network.models.RoverInfo
import com.sirelon.marsroverphotos.domain.models.CURIOSITY_ID
import com.sirelon.marsroverphotos.domain.models.INSIGHT_ID
import com.sirelon.marsroverphotos.domain.models.OPPORTUNITY_ID
import com.sirelon.marsroverphotos.domain.models.PERSEVERANCE_ID
import com.sirelon.marsroverphotos.domain.models.PhotosQueryRequest
import com.sirelon.marsroverphotos.domain.models.SPIRIT_ID
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

    suspend fun getRoversPhotos(query: PhotosQueryRequest): List<MarsImage> {
        // We should call another api if rover is insight or perseverance
        val sol = query.sol
        return when (query.roverId) {
            PERSEVERANCE_ID -> {
                loadPerseverance(query)
            }

            INSIGHT_ID -> {
                nasaApi.getInsightRawImages(from = "$sol:sol", to = "$sol:sol").list.mapToUi()
            }

            CURIOSITY_ID -> nasaApi.getRoverPhotos("Curiosity", sol, query.camera).list.mapToUi()

            OPPORTUNITY_ID -> nasaApi.getRoverPhotos(
                "Opportunity",
                sol,
                query.camera
            ).list.mapToUi()

            SPIRIT_ID -> nasaApi.getRoverPhotos("Spirit", sol, query.camera).list.mapToUi()

            else -> throw IllegalArgumentException("Unsupported rover id: ${query.roverId}")
        }
    }

    private suspend fun loadPerseverance(query: PhotosQueryRequest): List<MarsImage> {
        val response = nasaApi.getPerseveranceRawImages(
            sol = "${query.sol}:sol:in"
        )

        _perseveranceTotalImages.value = response.totalImages

        return response.photos.preveranceToUI()
    }

    suspend fun getRoverInfo(roverName: String): RoverInfo =
        nasaApi.getRoverInfo(roverName).roverInfo

    suspend fun getInsightLatestPhotos(): List<MarsImage> {
        return nasaApi.getInsightRawImages().list.mapToUi()
    }

    suspend fun getPerseveranceLatestPhotos(count: Int = 1): List<MarsImage> {
        val response = nasaApi.getPerseveranceRawImages(count = count)
        _perseveranceTotalImages.value = response.totalImages
        return response.photos.preveranceToUI()
    }
}
