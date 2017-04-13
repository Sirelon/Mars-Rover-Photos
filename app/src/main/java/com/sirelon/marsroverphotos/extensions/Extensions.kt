@file:JvmName("ExtensionsUtils")

package com.sirelon.marsroverphotos.extensions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.sirelon.marsroverphotos.R
import com.squareup.picasso.Picasso
import java.util.*

/**
 * @author romanishin
 * @since 31.10.16 on 11:26
 */
fun ViewGroup.inflate(layoutId: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(context).inflate(layoutId, this, attachToRoot)
}

infix fun ImageView.loadImage(imageUrl: String?) {
    if (TextUtils.isEmpty(imageUrl))
        this.setImageResource(R.drawable.img_placeholder)
    else {
//        Picasso.with(context).setIndicatorsEnabled(true)
        Picasso.with(context).load(imageUrl).placeholder(R.drawable.img_placeholder).into(this)
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
    val activeNetwork = connectivityManager.getActiveNetworkInfo()
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

inline fun <reified T : Any> DatabaseReference.setValueObservable(arg: T): io.reactivex
.Observable<T> {
    return io.reactivex.Observable.create<T> { emitter ->
        this.setValue(arg)
                .addOnSuccessListener {
                    emitter.onNext(arg)
                    emitter.onComplete()
                }
                .addOnFailureListener {
                    emitter.onError(it)
                }
    }
}

inline fun DatabaseReference.clearValueObservable(): io.reactivex.Observable<Boolean> {
    return io.reactivex.Observable.create<Boolean> { emitter ->
        this.setValue(null)
                .addOnSuccessListener {
                    emitter.onNext(true)
                    emitter.onComplete()
                }
                .addOnFailureListener {
                    emitter.onError(it)
                }
    }
}

inline fun DatabaseReference.singleEventFirebase(): io.reactivex.Observable<DataSnapshot> {
    return io.reactivex.Observable.create<DataSnapshot> { emitter ->
        this.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                emitter.onError(p0.toException())
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                emitter.onNext(dataSnapshot)
                emitter.onComplete()
            }
        })
    }
}

inline fun DatabaseReference.isExist(): io.reactivex.Observable<Boolean> {
    return singleEventFirebase().map { it.exists() }
}

fun Any?.logD(){
    Log.d("Sirelon", this?.toString() ?: "NULL")
}

fun Throwable.logE() {
    Log.e("Sirelon", this.message, this)
}
