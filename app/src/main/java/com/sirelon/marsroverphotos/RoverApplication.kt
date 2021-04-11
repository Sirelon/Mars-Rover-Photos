package com.sirelon.marsroverphotos

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.sirelon.marsroverphotos.tracker.FirebaseTracker
import com.sirelon.marsroverphotos.tracker.ITracker
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

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

        GlobalScope.launch {
            kotlin.runCatching {
                // Configurate ads
                MobileAds.initialize(this@RoverApplication) {
                    Log.d("RoverApplication", "On Add Init status $it")
                }
            }
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