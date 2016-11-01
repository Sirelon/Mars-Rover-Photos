package com.sirelon.marsroverphotos.network

import com.squareup.moshi.Json

/**
 * @author romanishin
 * @since 01.11.16 on 11:25
 */
class RoverResponse(
        @Json(name = "photo_manifest")
        var roverInfo: RoverInfo
)

class RoverInfo(
        @Json(name = "name")
        var name: String,

        @Json(name = "landing_date")
        var landingDate: String,

        @Json(name = "launch_date")
        var launchDate: String,

        @Json(name = "status")
        var status: String,

        @Json(name = "max_sol")
        var maxSol: Long,

        @Json(name = "max_date")
        var maxDate: String,

        @Json(name = "total_photos")
        var totalPhotos: Int)