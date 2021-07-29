package com.sirelon.marsroverphotos

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.sirelon.marsroverphotos.storage.Prefs
import com.sirelon.marsroverphotos.tracker.FirebaseTracker
import com.sirelon.marsroverphotos.tracker.ITracker
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber

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

        Prefs.init(this)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        FirebaseFirestore.getInstance().firestoreSettings = settings
    }

    val tracker: ITracker by lazy { FirebaseTracker(this) }

    val dataManger by lazy {
        DataManager(this, tracker)
    }
}