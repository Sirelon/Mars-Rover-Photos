package com.sirelon.marsroverphotos

import android.app.Application
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Logger
import com.jakewharton.picasso.OkHttp3Downloader
import com.sirelon.marsroverphotos.extensions.logE
import com.sirelon.marsroverphotos.tracker.AnswersTracker
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

        val fabric = Fabric.Builder(this).kits(Crashlytics(), Answers()).debuggable(BuildConfig.DEBUG).build()
        Fabric.with(fabric)

        FirebaseDatabase.getInstance().setLogLevel(if (BuildConfig.DEBUG) Logger.Level.DEBUG else Logger.Level.NONE)
        Picasso.setSingletonInstance(picasso())
    }

    val dataManger by lazy {
        DataManager(this, AnswersTracker())
    }

    private fun picasso(): Picasso = Picasso.Builder(this).listener { picasso, uri, exception ->
         exception?.logE()
     }.downloader(OkHttp3Downloader(this)).loggingEnabled(BuildConfig.DEBUG).build()
}