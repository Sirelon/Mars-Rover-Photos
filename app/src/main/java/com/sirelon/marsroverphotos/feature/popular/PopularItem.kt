package com.sirelon.marsroverphotos.feature.popular

import com.sirelon.marsroverphotos.adapter.AdapterConstants
import com.sirelon.marsroverphotos.models.ViewType

/**
 * Created on 9/21/17 10:25 PM for Mars-Rover-PhotosLocal.
 */
class PopularItem : ViewType{
    var totalPhotos: Int = 0

    override fun getViewType(): Int = AdapterConstants.POPULAR_ITEM
}