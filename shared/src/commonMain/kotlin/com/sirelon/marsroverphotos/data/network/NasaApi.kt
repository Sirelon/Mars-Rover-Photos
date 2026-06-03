package com.sirelon.marsroverphotos.data.network

import com.sirelon.marsroverphotos.data.network.models.NasaImagesSearchResponse
import com.sirelon.marsroverphotos.data.network.models.PerseverancePhotosResponse
import com.sirelon.marsroverphotos.data.network.models.PhotosResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * NASA API client for fetching Mars rover data.
 * Uses Ktor HttpClient with platform-specific engines.
 */
internal class NasaApi(private val ktor: HttpClient) {

    suspend fun getInsightRawImages(
        mission: String = "insight",
        from: String? = null,
        to: String? = null,
    ): PhotosResponse {
        return ktor.get("https://mars.nasa.gov/api/v1/raw_image_items/?order=sol+desc%2Cdate_taken+desc&per_page=100&page=0&condition_1=$mission:mission") {
            parameter("condition_2", from)
            parameter("condition_3", to)
        }.body()
    }

    suspend fun searchImages(
        q: String,
        page: Int,
        pageSize: Int = 100,
    ): NasaImagesSearchResponse {
        return ktor.get("https://images-api.nasa.gov/search") {
            parameter("q", q)
            parameter("media_type", "image")
            parameter("page", page)
            parameter("page_size", pageSize)
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
