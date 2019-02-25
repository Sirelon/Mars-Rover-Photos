package com.sirelon.marsroverphotos.models

import android.view.View
import androidx.core.util.Pair

/**
 * @author romanishin
 * @since 31.10.16 on 17:01
 */
interface OnModelChooseListener<TYPE : ViewType> {

    fun onModelChoose(model: TYPE, vararg sharedElements: Pair<View, String>)

}