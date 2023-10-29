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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNames
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

@Serializable
data class User(
    @JsonNames("uid")
    val id: List<String>
)

fun handleJsonWithJsonNames() {
//    val json = """{"uid": ["1", "2", "3"]}"""
//    val user = Json.decodeFromString<User>(json)
    val json =
        """{"items":[{"id":11378,"camera_vector":null,"site":null,"imageid":"C000M0190_613408865EDR_F0000_0461M_","subframe_rect":"(1,1,1024,1024)","sol":190,"scale_factor":1,"camera_model_component_list":null,"instrument":"icc","url":"https://mars.nasa.gov/insight-raw-images/surface/sol/0190/icc/C000M0190_613408865EDR_F0000_0461M_.JPG","spacecraft_clock":613408865.55938,"attitude":null,"camera_position":null,"camera_model_type":null,"drive":null,"xyz":null,"created_at":"2019-06-11T02:00:28.945Z","updated_at":"2019-06-11T02:00:28.945Z","mission":"insight","extended":{"localtime":"16:22:28.916"},"date_taken":"2019-06-10T03:21:57.044Z","date_received":"2019-06-11T01:39:02.102Z","instrument_sort":99,"sample_type_sort":99,"is_thumbnail":false,"title":"Sol 190: Instrument Context Camera (ICC)","description":"NASA's InSight Mars lander acquired this image of the area in front of the lander using its lander-mounted, Instrument Context Camera (ICC).\u003cbr /\u003e\u003cbr /\u003eThis image was acquired on June 10, 2019, Sol 190 of the InSight mission where the local mean solar time for the image exposures was 16:22:28.916 PM. Each ICC image has a field of view of 124 x 124 degrees.","link":"/raw_images/11378","image_credit":"NASA/JPL-Caltech","https_url":"https://mars.nasa.gov/insight-raw-images/surface/sol/0190/icc/C000M0190_613408865EDR_F0000_0461M_.JPG"}],"more":false,"total":1,"page":0,"per_page":100}"""
    val user = Json.decodeFromString<PhotosResponse>(json)
    print(user)
}

fun main() {
    handleJsonWithJsonNames()
}

private val json = Json {
    isLenient = true
    ignoreUnknownKeys = true
}

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