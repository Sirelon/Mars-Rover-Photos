package com.sirelon.marsroverphotos.data.network.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Collection+JSON wrapper returned by https://images-api.nasa.gov/search */
@Serializable
data class NasaImagesSearchResponse(
    @SerialName("collection")
    val collection: NasaImagesCollection,
)

@Serializable
data class NasaImagesCollection(
    @SerialName("items")
    val items: List<NasaImagesItem> = emptyList(),
    @SerialName("metadata")
    val metadata: NasaImagesMetadata? = null,
    @SerialName("links")
    val links: List<NasaImagesCollectionLink>? = null,
)

@Serializable
data class NasaImagesItem(
    @SerialName("data")
    val data: List<NasaImagesItemData> = emptyList(),
    @SerialName("links")
    val links: List<NasaImagesItemLink>? = null,
)

@Serializable
data class NasaImagesItemData(
    @SerialName("nasa_id")
    val nasaId: String,
    @SerialName("title")
    val title: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("date_created")
    val dateCreated: String? = null,
    @SerialName("center")
    val center: String? = null,
    @SerialName("keywords")
    val keywords: List<String>? = null,
)

@Serializable
data class NasaImagesItemLink(
    @SerialName("href")
    val href: String,
    @SerialName("render")
    val render: String? = null,
)

@Serializable
data class NasaImagesMetadata(
    @SerialName("total_hits")
    val totalHits: Int = 0,
)

@Serializable
data class NasaImagesCollectionLink(
    @SerialName("rel")
    val rel: String,
    @SerialName("href")
    val href: String,
)
