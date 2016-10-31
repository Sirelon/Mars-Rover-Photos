package com.sirelon.marsroverphotos.models

import com.sirelon.marsroverphotos.adapter.AdapterConstants
import com.squareup.moshi.Json

/**
 * @author romanishin
 * @since 31.10.16 on 11:19
 */
data class MarsPhoto(
        @Json (name = "id")
        val id: Long,

        @Json (name = "sol")
        val sol: Long,

        val name: String,

        @Json (name = "img_src")
        val imageUrl: String,

        @Json (name = "earth_date")
        val earthDate: String,

        @Json (name = "rover")
        val rover: Rover) : ViewType {

    override fun getViewType(): Int = AdapterConstants.MARS_PHOTO

}