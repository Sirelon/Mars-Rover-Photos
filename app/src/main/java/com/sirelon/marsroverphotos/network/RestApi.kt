package com.sirelon.marsroverphotos.network

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.sirelon.marsroverphotos.feature.photos.mapToUi
import com.sirelon.marsroverphotos.feature.photos.preveranceToUI
import com.sirelon.marsroverphotos.feature.rovers.Curiosity_ID
import com.sirelon.marsroverphotos.feature.rovers.INSIGHT_ID
import com.sirelon.marsroverphotos.feature.rovers.Opportunity_ID
import com.sirelon.marsroverphotos.feature.rovers.PERSEVARANCE_ID
import com.sirelon.marsroverphotos.feature.rovers.Spirit_ID
import com.sirelon.marsroverphotos.models.PhotosQueryRequest
import com.sirelon.marsroverphotos.storage.MarsImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

/**
 * @author romanishin
 * @since 31.10.16 on 15:28
 */
class RestApi(context: Context) {
    private val nasaApi: NasaApi

    private val _perseveranceTotalImages = MutableStateFlow<Long?>(null)
    val perseveranceTotalImages = _perseveranceTotalImages.filterNotNull().distinctUntilChanged()

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    init {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)

        val okkClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

        val contentType = "application/json".toMediaType()


        val retrofit = Retrofit.Builder()
            .client(okkClient)
            .baseUrl("https://api.nasa.gov")
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()

        nasaApi = retrofit.create(NasaApi::class.java)
    }

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
//        val dateUtil = query.dateUtil
//        val dateFrom = dateUtil.dateFromSol(query.sol)
//        val dateTo = dateUtil.dateFromSol(query.sol)
        val response = nasaApi.getPerseveranceRawImages(
//            from = "${dateUtil.parseTime(dateFrom)}:date_taken:gte",
//            to = "${dateUtil.parseTime(dateTo)}:date_taken:lt"
            sol = "${query.sol}:sol:in"
        )

        _perseveranceTotalImages.value = response.totalImages

        return response.photos.preveranceToUI()
    }

    suspend fun getRoverInfo(roverName: String): RoverInfo =
        nasaApi.getRoverInfo(roverName).roverInfo
}