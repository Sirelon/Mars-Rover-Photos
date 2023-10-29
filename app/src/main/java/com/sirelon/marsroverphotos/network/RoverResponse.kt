package com.sirelon.marsroverphotos.network

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @author romanishin
 * @since 01.11.16 on 11:25
 */
@Keep
@Serializable
data class RoverResponse(
        @SerialName(value = "photo_manifest")
        var roverInfo: RoverInfo
)

@Keep
@Serializable
data class RoverInfo(
        @SerialName(value = "name")
        var name: String,

        @SerialName(value = "landing_date")
        var landingDate: String,

        @SerialName(value = "launch_date")
        var launchDate: String,

        @SerialName(value = "status")
        var status: String,

        @SerialName(value = "max_sol")
        var maxSol: Long,

        @SerialName(value = "max_date")
        var maxDate: String,

        @SerialName(value = "total_photos")
        var totalPhotos: Int)