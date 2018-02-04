@file:JvmName("ExtensionsUtils")

package com.sirelon.marsroverphotos.extensions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.provider.Settings
import android.support.v4.content.ContextCompat
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.RoverApplication
import com.squareup.picasso.Picasso
import java.util.*

/**
 * @author romanishin
 * @since 31.10.16 on 11:26
 */
fun ViewGroup.inflate(layoutId: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(context).inflate(layoutId, this, attachToRoot)
}

object placeholder {
    val drawable: Drawable by lazy {
        ContextCompat.getDrawable(RoverApplication.APP, R.drawable.img_placeholder)!!
    }
}


infix fun ImageView.loadImage(imageUrl: String?) {
    if (TextUtils.isEmpty(imageUrl))
        this.setImageDrawable(placeholder.drawable)
    else {
        Picasso.with(context).load(imageUrl).placeholder(placeholder.drawable).into(this)
    }
}

fun Activity.showAppSettings() {
    val intent = Intent()
    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
    val uri = Uri.fromParts("package", this.packageName, null)
    intent.data = uri
    this.startActivityForResult(intent, 7898)
}

fun Activity.isConnected(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork = connectivityManager.activeNetworkInfo
    return activeNetwork != null && activeNetwork.isConnectedOrConnecting
}

fun Int.random(max: Int): Int {
    val random = Random()
    val min = this
    return random.nextInt((max - min).toInt() + 1) + min
}

// Inline function to create Parcel Creator
inline fun <reified T : Parcelable> createParcel(
        crossinline createFromParcel: (Parcel) -> T?): Parcelable.Creator<T> =
        object : Parcelable.Creator<T> {
            override fun createFromParcel(source: Parcel): T? = createFromParcel(source)
            override fun newArray(size: Int): Array<out T?> = arrayOfNulls(size)
        }

fun Any?.logD() {
    Log.d("Sirelon", this?.toString() ?: "NULL")
}

fun Throwable.logE() {
    Log.e("Sirelon", this.message, this)
}
