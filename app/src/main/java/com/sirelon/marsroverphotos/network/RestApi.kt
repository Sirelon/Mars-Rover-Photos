package com.sirelon.marsroverphotos.network

import android.content.Context
import com.readystatesoftware.chuck.ChuckInterceptor
import com.sirelon.marsroverphotos.models.PhotosQueryRequest
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
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
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()

        nasaApi = retrofit.create(NasaApi::class.java)
    }

    fun getRoversPhotos(query: PhotosQueryRequest): Call<PhotosResponse> {
        return nasaApi.getRoverPhotos(query.roverName, query.sol, query.camera)
    }

    fun getRoverInfo(roverName: String): Call<RoverResponse>{
        return nasaApi.getRoverInfo(roverName)
    }
}