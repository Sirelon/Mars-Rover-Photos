package com.sirelon.marsroverphotos.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

internal class NasaApi(private val ktor: HttpClient) {

    private companion object {
        private val APIKEY = "zyVCPOCqaNBoEambV3n4awUf6TuaPZsHn1trst5E"
        private val APIKEY_ARG = "api_key"
    }

    suspend fun getRoverInfo(
        roverName: String,
        apiKey: String = APIKEY
    ): RoverResponse {
        return ktor.get("/mars-photos/api/v1/manifests/$roverName") {
            parameter(APIKEY_ARG, apiKey)
        }.body()
    }

    suspend fun getRoverPhotos(
        roverName: String,
        sol: Long?,
        camera: String?,
        apiKey: String = APIKEY,
    ): PhotosResponse {
        return ktor.get("/mars-photos/api/v1/rovers/$roverName/photos") {
            parameter(APIKEY_ARG, apiKey)
            parameter("sol", sol)
            parameter("camera", camera)
        }.body()
    }

    suspend fun getInsightRawImages(
        from: String? = null,
        to: String? = null,
    ): PhotosResponse {
        return ktor.get("https://mars.nasa.gov/api/v1/raw_image_items/?order=sol+desc%2Cdate_taken+desc&per_page=100&page=0&condition_1=insight:mission") {
            parameter("condition_2", from)
            parameter("condition_3", to)
        }.body()
    }

    suspend fun getPerseveranceRawImages(
        count: Int = 500,
        sol: String? = null,
    ): PerseverancePhotosResponse {
        return ktor.get("https://mars.nasa.gov/rss/api/?feed=raw_images&category=mars2020&feedtype=json&page=0&order=sol+desc") {
            parameter("num", count)
            parameter("condition_3", sol)
        }.body()
    }
}