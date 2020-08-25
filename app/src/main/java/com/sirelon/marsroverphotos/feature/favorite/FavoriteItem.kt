package com.sirelon.marsroverphotos.feature.favorite

import com.sirelon.marsroverphotos.adapter.AdapterConstants
import com.sirelon.marsroverphotos.models.ViewType
import com.sirelon.marsroverphotos.storage.MarsImage

/**
 * Created on 9/21/17 10:25 PM for Mars-Rover-PhotosLocal.
 */
class FavoriteItem(val image: MarsImage) : ViewType{

    override fun getViewId() = this

    override fun getViewType(): Int = AdapterConstants.FAVORITES
}