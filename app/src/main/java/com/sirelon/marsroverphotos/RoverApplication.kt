package com.sirelon.marsroverphotos

import android.app.Application
import androidx.multidex.MultiDex
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.google.android.gms.ads.MobileAds
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.sirelon.marsroverphotos.tracker.AnswersTracker
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

        if (BuildConfig.DEBUG){
            MultiDex.install(this)
        }
        // Configurate ads
        MobileAds.initialize(this, getString(R.string.ad_application_id))

        val fabric = Fabric.Builder(this).kits(Crashlytics(), Answers()).debuggable(BuildConfig.DEBUG).build()
        Fabric.with(fabric)

        val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
        FirebaseFirestore.getInstance().firestoreSettings = settings
    }

    val dataManger by lazy {
        DataManager(this, AnswersTracker())
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
    }
}