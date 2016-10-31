@file:JvmName("ExtensionsUtils")

package com.sirelon.marsroverphotos

import android.os.Parcel
import android.os.Parcelable
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
    else {
        Picasso.with(context).setIndicatorsEnabled(true)
        Picasso.with(context).load(imageUrl).placeholder(R.drawable.img_placeholder).into(this)
    }
}

// Inline function to create Parcel Creator
inline fun <reified T : Parcelable> createParcel(
        crossinline createFromParcel: (Parcel) -> T?): Parcelable.Creator<T> =
        object : Parcelable.Creator<T> {
            override fun createFromParcel(source: Parcel): T? = createFromParcel(source)
            override fun newArray(size: Int): Array<out T?> = arrayOfNulls(size)
        }