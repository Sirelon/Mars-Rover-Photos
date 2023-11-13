@file:JvmName("ExtensionsUtils")

package com.sirelon.marsroverphotos.extensions

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler

/**
 * @author romanishin
 * @since 31.10.16 on 11:26
 */
fun Activity.showAppSettings() {
    val intent = Intent()
    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
    val uri = Uri.fromParts("package", this.packageName, null)
    intent.data = uri
    kotlin.runCatching { this.startActivityForResult(intent, 7898) }
}

// Inline function to create Parcel Creator
inline fun <reified T : Parcelable> createParcel(
    crossinline createFromParcel: (Parcel) -> T?
): Parcelable.Creator<T> =
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

fun recordException(e: Throwable) {
    e.printStackTrace()
//    FirebaseCrashlytics.getInstance().recordException(e)
}

val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
    recordException(throwable)
}