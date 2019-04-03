package com.sirelon.marsroverphotos.adapter.headers

import com.sirelon.marsroverphotos.adapter.AdapterConstants
import com.sirelon.marsroverphotos.models.ViewType

/**
 * @author romanishin
 * @since 01.11.16 on 16:18
 */
class HeaderViewType : ViewType{

    override fun getViewId() = this

    override fun getViewType(): Int = AdapterConstants.HEADER
}