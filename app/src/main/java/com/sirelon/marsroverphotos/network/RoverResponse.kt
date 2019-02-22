package com.sirelon.marsroverphotos.network

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * @author romanishin
 * @since 01.11.16 on 11:25
 */
@Keep
data class RoverResponse(
        @SerializedName(value = "photo_manifest")
        var roverInfo: RoverInfo
)

@Keep
data class RoverInfo(
        @SerializedName(value = "name")
        var name: String,

        @SerializedName(value = "landing_date")
        var landingDate: String,

        @SerializedName(value = "launch_date")
        var launchDate: String,

        @SerializedName(value = "status")
        var status: String,

        @SerializedName(value = "max_sol")
        var maxSol: Long,

        @SerializedName(value = "max_date")
        var maxDate: String,

        @SerializedName(value = "total_photos")
        var totalPhotos: Int)