package com.sirelon.marsroverphotos.network

import android.content.Context
import com.readystatesoftware.chuck.ChuckInterceptor
import com.sirelon.marsroverphotos.feature.photos.mapToUi
import com.sirelon.marsroverphotos.feature.photos.preveranceToUI
import com.sirelon.marsroverphotos.feature.rovers.INSIGHT_ID
import com.sirelon.marsroverphotos.feature.rovers.PERSEVARANCE_ID
import com.sirelon.marsroverphotos.models.PhotosQueryRequest
import com.sirelon.marsroverphotos.storage.MarsImage
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * @author romanishin
 * @since 31.10.16 on 15:28
 */

class RestApi(context: Context) {
    private val nasaApi: NasaApi

    init {
        val okkClient = OkHttpClient.Builder()
            .addInterceptor(ChuckInterceptor(context))
            .build()

        val retrofit = Retrofit.Builder()
            .client(okkClient)
            .baseUrl("https://api.nasa.gov")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        nasaApi = retrofit.create(NasaApi::class.java)
    }

    suspend fun getRoversPhotos(query: PhotosQueryRequest): List<MarsImage> {
        // We should call another api if rover is insight
        val sol = query.sol
        val roverName = query.rover.name
        val list = if (query.rover.id == PERSEVARANCE_ID) {
            return loadPerseverance(query)
        } else if (query.rover.id == INSIGHT_ID) {
            nasaApi.getInsightRawImages(from = "$sol:sol", to = "$sol:sol").photos.mapToUi()
        } else {
            nasaApi.getRoverPhotos(roverName, sol, query.camera).photos.mapToUi()
        }
        return list
    }

    private suspend fun loadPerseverance(query: PhotosQueryRequest): List<MarsImage> {
        val dateUtil = query.dateUtil
        val dateFrom = dateUtil.dateFromSol(query.sol)
        val dateTo = dateUtil.dateFromSol(query.sol)
        val response = nasaApi.getPerseveranceRawImages(
            from = "${dateUtil.parseTime(dateFrom)}:date_taken:gte",
            to = "${dateUtil.parseTime(dateTo)}:date_taken:lt"
        )

        return response.photos.preveranceToUI()
    }

    suspend fun getRoverInfo(roverName: String): RoverInfo =
        nasaApi.getRoverInfo(roverName).roverInfo
}