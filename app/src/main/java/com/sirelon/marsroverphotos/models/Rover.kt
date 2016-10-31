package com.sirelon.marsroverphotos.models

import com.sirelon.marsroverphotos.adapter.AdapterConstants
import com.squareup.moshi.Json

/**
 * @author romanishin
 * @since 31.10.16 on 15:14
 */
data class Rover(
        @Json(name = "id")
        var id: Long,

        @Json(name = "name")
        var name: String,

        var iamgeUrl: String?,

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
        var totalPhotos: Int) : ViewType {

    override fun getViewType(): Int = AdapterConstants.ROVER

}