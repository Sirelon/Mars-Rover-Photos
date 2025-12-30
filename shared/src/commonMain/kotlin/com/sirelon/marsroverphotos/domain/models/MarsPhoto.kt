package com.sirelon.marsroverphotos.domain.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/**
 * @author romanishin
 * @since 31.10.16 on 11:19
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class MarsPhoto(
    @SerialName(value = "id")
    val id: String,

    @SerialName(value = "sol")
    val sol: Long,

    @JsonNames("name", "title")
    val name: String?,

    @JsonNames("img_src", "url")
    val imageUrl: String,

    @JsonNames("earth_date", "created_at")
    val earthDate: String,

    @SerialName(value = "camera")
    val camera: RoverCamera?
)
