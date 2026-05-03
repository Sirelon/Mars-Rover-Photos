package com.sirelon.marsroverphotos.data.network.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @author romanishin
 * @since 01.11.16 on 11:25
 */
@Serializable
data class RoverResponse(
    @SerialName(value = "photo_manifest")
    val roverInfo: RoverInfo
)

@Serializable
data class RoverInfo(
    @SerialName(value = "name")
    val name: String,

    @SerialName(value = "landing_date")
    val landingDate: String,

    @SerialName(value = "launch_date")
    val launchDate: String,

    @SerialName(value = "status")
    val status: String,

    @SerialName(value = "max_sol")
    val maxSol: Long,

    @SerialName(value = "max_date")
    val maxDate: String,

    @SerialName(value = "total_photos")
    val totalPhotos: Int
)
