package com.sirelon.marsroverphotos.network

import android.content.Context
import com.readystatesoftware.chuck.ChuckInterceptor
import com.sirelon.marsroverphotos.models.PhotosQueryRequest
import okhttp3.OkHttpClient
import retrofit2.Call
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

    suspend fun getRoversPhotos(query: PhotosQueryRequest): PhotosResponse {
        // We should call another api if rover is insight
        val sol = query.sol
        if (query.roverName.toLowerCase() == "insight") {
            return nasaApi.getInsightRawImages(from = "$sol:sol", to = "$sol:sol")
        }
        return nasaApi.getRoverPhotos(query.roverName, sol, query.camera)
    }

    suspend fun getRoverInfo(roverName: String): RoverInfo =
        nasaApi.getRoverInfo(roverName).roverInfo
}