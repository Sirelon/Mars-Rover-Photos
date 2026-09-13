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

    suspend fun getRawImages(
        mission: String,
        from: String? = null,
        to: String? = null,
        page: Int = 0,
    ): PhotosResponse {
        return ktor.get("https://mars.nasa.gov/api/v1/raw_image_items/?order=sol+desc%2Cdate_taken+desc&per_page=100&page=0&condition_1=$mission:mission") {
            parameter("condition_2", from)
            parameter("condition_3", to)
            parameter("page", page)
        }.body()
    }

    suspend fun searchImages(
        q: String,
        page: Int,
        pageSize: Int = 100,
        keywords: String? = null,
    ): NasaImagesSearchResponse {
        return ktor.get("https://images-api.nasa.gov/search") {
            parameter("q", q)
            parameter("media_type", "image")
            parameter("page", page)
            parameter("page_size", pageSize)
            // Ktor drops null parameters, so keywords is only sent when provided.
            parameter("keywords", keywords)
        }.body()
    }

    /**
     * One page of the Mars 2020 raw-image RSS feed. [category] selects the mission within that
     * feed: `mars2020` for Perseverance, `ingenuity` for the helicopter that flew alongside it.
     *
     * `category` and `order` are sent as parameters rather than baked into the URL literal on
     * purpose. This endpoint does NOT resolve a duplicated query parameter as "last wins" — it
     * silently falls back to its own default (`mars2020`, `sol desc`). A second `category` here
     * would therefore return Perseverance photos for an Ingenuity request, with no error, and
     * they would be cached under the wrong rover id. Never reintroduce either into the literal.
     */
    suspend fun getPerseveranceRawImages(
        count: Int = 100,
        sol: String? = null,
        category: String = "mars2020",
        page: Int? = null,
    ): PerseverancePhotosResponse {
        return ktor.get("https://mars.nasa.gov/rss/api/?feed=raw_images&feedtype=json") {
            parameter("category", category)
            parameter("order", "sol desc")
            parameter("num", count)
            parameter("condition_3", sol)
            parameter("page", page)
        }.body()
    }

    /**
     * First sol at or after [fromSol] that has an Ingenuity photo, resolved in a single request
     * via the feed's undocumented `gte` range operator, or null when there is none.
     *
     * Deliberately its own `ktor.get` with its own URL: this lookup needs `order=sol asc`, and
     * [getPerseveranceRawImages] fixes `sol desc`. Sending `order` twice does not override —
     * the feed falls back to its default and answers with the newest sol instead of the nearest,
     * which is a plausible wrong answer rather than an error. Keep the two calls separate.
     *
     * `condition_2` is the feed's "from" slot and `condition_3` its "to" slot; the pairing
     * matches [getRawImages]'s own from/to parameters.
     */
    suspend fun getIngenuityNearestSolAtOrAfter(fromSol: Long): Long? {
        return ktor.get("https://mars.nasa.gov/rss/api/?feed=raw_images&feedtype=json&num=1") {
            parameter("category", "ingenuity")
            parameter("order", "sol asc")
            parameter("condition_2", "$fromSol:sol:gte")
        }.body<PerseverancePhotosResponse>().photos.firstOrNull()?.sol
    }
}
