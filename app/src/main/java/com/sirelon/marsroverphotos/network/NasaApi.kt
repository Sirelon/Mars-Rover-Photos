package com.sirelon.marsroverphotos.network

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
    ): Call<RoverResponse>

    @GET("/mars-photos/api/v1/rovers/{roverName}/photos")
    fun getRoverPhotos(
        @Path("roverName") roverName: String,
        @Query("sol") sol: Long?,
        @Query("camera") camera: String?,
        @Query("api_key") apiKey: String = APIKEY
    ): Call<PhotosResponse>

    // TODO
    @GET("https://mars.nasa.gov/api/v1/raw_image_items/?order=sol+desc%2Cdate_taken+desc&per_page=100&page=0&condition_1=insight%3Amission&search=&extended=")
    fun getRawImages(
        @Query("begin_sol") fromSol: Long? = null,
        @Query("end_sol") toSol: Long? = null,
        @Query("begin_date") fromDate: String? = null,
        @Query("end_date") toDate: String? = null
    ): Call<PhotosResponse>

}