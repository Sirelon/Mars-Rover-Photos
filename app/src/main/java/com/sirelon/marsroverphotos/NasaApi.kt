package com.sirelon.marsroverphotos

import com.sirelon.marsroverphotos.models.MarsPhoto
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * @author romanishin
 * @since 31.10.16 on 15:20
 */
interface NasaApi {

    @GET("/mars-photos/api/v1/rovers/curiosity/photos")
    fun getCuriosityPhotos(
            @Query("sol") sol : Int,
            @Query("camera") camera : String,
            @Query("api_key") apiKey : String = "DEMO_KEY") : Call<List<MarsPhoto>>


}