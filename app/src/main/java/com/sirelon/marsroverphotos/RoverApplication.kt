package com.sirelon.marsroverphotos

import android.app.Application
import com.crashlytics.android.Crashlytics
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Logger
import com.jakewharton.picasso.OkHttp3Downloader
import com.sirelon.marsroverphotos.extensions.logE
import com.squareup.picasso.Picasso
import io.fabric.sdk.android.Fabric

/**
 * @author romanishin
 * @since 16.11.16 on 12:17
 */
class RoverApplication : Application() {

    companion object {
        lateinit var APP: RoverApplication
    }

    override fun onCreate() {
        super.onCreate()
        APP = this

        if (!BuildConfig.DEBUG)
            Fabric.with(this, Crashlytics())

        FirebaseDatabase.getInstance().setLogLevel(if (BuildConfig.DEBUG) Logger.Level.DEBUG else Logger.Level.NONE)
    }

    val dataManger by lazy {
        DataManager(this)
    }

    fun picasso(): Picasso = Picasso.Builder(this).listener { picasso, uri, exception ->
         exception?.logE()
     }.downloader(OkHttp3Downloader(this)).build()
}