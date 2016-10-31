package com.sirelon.marsroverphotos.network

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * @author romanishin
 * @since 31.10.16 on 15:28
 */

class RestApi {
    private val nasaApi: NasaApi

    init {
        val retrofit = Retrofit.Builder()
                .baseUrl("https://api.nasa.gov")
                .addConverterFactory(MoshiConverterFactory.create())
                .build()

        nasaApi = retrofit.create(NasaApi::class.java)
    }

    fun getCuriosityPhotos(sol: Int, camera: String): Call<PhotosResponse> {
        return nasaApi.getCuriosityPhotos(sol, camera)
    }
}