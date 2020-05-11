package com.sirelon.marsroverphotos

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.multidex.MultiDex
import com.google.android.gms.ads.MobileAds
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.sirelon.marsroverphotos.tracker.AnswersTracker
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.conscrypt.Conscrypt
import java.security.Security


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

        if (BuildConfig.DEBUG) {
            MultiDex.install(this)
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Security.insertProviderAt(Conscrypt.newProvider(), 1)
        }

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

    val dataManger by lazy {
        DataManager(this, AnswersTracker())
    }
}