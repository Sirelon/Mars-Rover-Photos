package com.sirelon.marsroverphotos

import android.app.Application
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.google.android.gms.ads.MobileAds
import com.google.firebase.firestore.FirebaseFirestore
import com.jakewharton.picasso.OkHttp3Downloader
import com.sirelon.marsroverphotos.extensions.logE
import com.sirelon.marsroverphotos.tracker.AnswersTracker
import com.squareup.picasso.Picasso
import io.fabric.sdk.android.Fabric
import com.google.firebase.firestore.FirebaseFirestoreSettings



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

        // Configurate ads
        MobileAds.initialize(this, getString(R.string.ad_application_id))

        val fabric = Fabric.Builder(this).kits(Crashlytics(), Answers()).debuggable(BuildConfig.DEBUG).build()
        Fabric.with(fabric)

        val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
        FirebaseFirestore.getInstance().firestoreSettings = settings

        Picasso.setSingletonInstance(picasso())
    }

    val dataManger by lazy {
        DataManager(this, AnswersTracker())
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
    }

    private fun picasso(): Picasso = Picasso.Builder(this).listener { _, _, exception ->
        exception?.logE()
    }.downloader(OkHttp3Downloader(this)).loggingEnabled(BuildConfig.DEBUG).build()
}