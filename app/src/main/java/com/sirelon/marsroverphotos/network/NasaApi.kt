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

    @GET("/mars-photos/api/v1/rovers/{roverName}/photos")
    fun getRoverPhotos(
            @Path("roverName") roverName: String,
            @Query("sol") sol: Long?,
            @Query("camera") camera: String?,
            @Query("api_key") apiKey: String = APIKEY): Call<PhotosResponse>

    @GET("/mars-photos/api/v1/manifests/{roverName}")
    fun getRoverInfo(
            @Path("roverName") roverName: String,
            @Query("api_key") apiKey: String = APIKEY): Call<RoverResponse>
}