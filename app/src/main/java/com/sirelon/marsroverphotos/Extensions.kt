@file:JvmName("ExtensionsUtils")

package com.sirelon.marsroverphotos

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.squareup.picasso.Picasso

/**
 * @author romanishin
 * @since 31.10.16 on 11:26
 */

fun ViewGroup.inflate(layoutId: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(context).inflate(layoutId, this, attachToRoot)
}

fun ImageView.loadImage(imageUrl: String?) {
    if (TextUtils.isEmpty(imageUrl))
        this.setImageResource(R.drawable.img_placeholder)
    else
        Picasso.with(context).load(imageUrl).into(this)
}