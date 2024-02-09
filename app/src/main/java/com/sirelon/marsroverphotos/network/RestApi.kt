package com.sirelon.marsroverphotos.network

import com.sirelon.marsroverphotos.feature.photos.mapToUi
import com.sirelon.marsroverphotos.feature.photos.preveranceToUI
import com.sirelon.marsroverphotos.feature.rovers.Curiosity_ID
import com.sirelon.marsroverphotos.feature.rovers.INSIGHT_ID
import com.sirelon.marsroverphotos.feature.rovers.Opportunity_ID
import com.sirelon.marsroverphotos.feature.rovers.PERSEVARANCE_ID
import com.sirelon.marsroverphotos.feature.rovers.Spirit_ID
import com.sirelon.marsroverphotos.models.PhotosQueryRequest
import com.sirelon.marsroverphotos.storage.MarsImage
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * @author romanishin
 * @since 31.10.16 on 15:28
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

    private val ktor = HttpClient(OkHttp) {
        // default validation to throw exceptions for non-2xx responses
        expectSuccess = true
        engine {
            // add logging interceptor
            addInterceptor(HttpLoggingInterceptor().apply {
                setLevel(
                    HttpLoggingInterceptor.Level.BODY
                )
            })
        }

        // set default request parameters
        defaultRequest {
            // add base url for all request
            url("https://api.nasa.gov")
        }

        install(ContentNegotiation) {
            json(json)
        }

        install(HttpTimeout) {
            val timeout = TimeUnit.MINUTES.toMillis(2)
            connectTimeoutMillis = timeout
            requestTimeoutMillis = timeout
            socketTimeoutMillis = timeout
        }
    }

    private val nasaApi: NasaApi = NasaApi(ktor)

    suspend fun getRoversPhotos(query: PhotosQueryRequest): List<MarsImage> {
        // We should call another api if rover is insight
        val sol = query.sol
        val list = when (query.roverId) {
            PERSEVARANCE_ID -> {
                return loadPerseverance(query)
            }

            INSIGHT_ID -> {
                nasaApi.getInsightRawImages(from = "$sol:sol", to = "$sol:sol").list.mapToUi()
            }

            Curiosity_ID -> nasaApi.getRoverPhotos("Curiosity", sol, query.camera).list.mapToUi()
            Opportunity_ID -> nasaApi.getRoverPhotos(
                "Opportunity",
                sol,
                query.camera
            ).list.mapToUi()

            Spirit_ID -> nasaApi.getRoverPhotos("Spirit", sol, query.camera).list.mapToUi()
            else -> throw IllegalArgumentException("Unsupported id")
        }
        return list
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
}