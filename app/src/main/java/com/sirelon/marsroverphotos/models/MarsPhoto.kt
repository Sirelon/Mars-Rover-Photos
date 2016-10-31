package com.sirelon.marsroverphotos.models

import com.sirelon.marsroverphotos.adapter.AdapterConstants

/**
 * @author romanishin
 * @since 31.10.16 on 11:19
 */
data class MarsPhoto(val id: Long, val sol: Long, val name: String, val imageUrl: String, val earthDate: String, val rover: Rover) : ViewType {

    override fun getViewType(): Int = AdapterConstants.MARS_PHOTO

}