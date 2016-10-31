@file:JvmName("ExtensionsUtils")

package com.sirelon.marsroverphotos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

/**
 * @author romanishin
 * @since 31.10.16 on 11:26
 */

fun ViewGroup.inflate(layoutId: Int, attachToRoot: Boolean = false) : View{
    return LayoutInflater.from(context).inflate(layoutId, this, attachToRoot)
}