package com.sirelon.marsroverphotos.network

import io.reactivex.Single
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * @author romanishin
 * @since 31.10.16 on 15:20
 */
interface NasaApi {

    companion object {
        val APIKEY = "zyVCPOCqaNBoEambV3n4awUf6TuaPZsHn1trst5E"
    }

    @GET("/mars-photos/api/v1/manifests/{roverName}")
    fun getRoverInfo(
        @Path("roverName") roverName: String,
        @Query("api_key") apiKey: String = APIKEY
    ): Single<RoverResponse>

    @GET("/mars-photos/api/v1/rovers/{roverName}/photos")
    fun getRoverPhotos(
        @Path("roverName") roverName: String,
        @Query("sol") sol: Long?,
        @Query("camera") camera: String?,
        @Query("api_key") apiKey: String = APIKEY
    ): Call<PhotosResponse>

    @GET("https://mars.nasa.gov/api/v1/raw_image_items/?order=sol+desc%2Cdate_taken+desc&per_page=100&page=0&condition_1=insight:mission")
    fun getRawImages(
        @Query("condition_2") from: String? = null,
        @Query("condition_3") to: String? = null
    ): Call<PhotosResponse>

}