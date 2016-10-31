package com.sirelon.marsroverphotos.models

import com.sirelon.marsroverphotos.adapter.AdapterConstants

/**
 * @author romanishin
 * @since 31.10.16 on 11:19
 */
data class MarsPhoto(var name: String, var imageUrl: String) : ViewType {

    override fun getViewType(): Int = AdapterConstants.MARS_PHOTO

}