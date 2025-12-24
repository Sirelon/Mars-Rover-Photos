package com.sirelon.marsroverphotos

import android.app.Application
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.sirelon.marsroverphotos.di.appModule
import com.sirelon.marsroverphotos.storage.Prefs
import com.sirelon.marsroverphotos.tracker.FirebaseTracker
import com.sirelon.marsroverphotos.tracker.ITracker
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

/**
 * @author romanishin
 * @since 16.11.16 on 12:17
 */
class RoverApplication : Application() {

    companion object {
        lateinit var APP: RoverApplication
    }

    val adEnabled = true

    override fun onCreate() {
        Log.d("Sirelon", "ON CREATE APP $this")
        super.onCreate()

        APP = this

        Prefs.init(this)

        startKoin {
            androidContext(this@RoverApplication)
            modules(appModule)
        }

//        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
//        }

        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
            .build()
        FirebaseFirestore.getInstance().firestoreSettings = settings
    }

    val tracker: ITracker by lazy { FirebaseTracker(this) }

    val dataManger by lazy {
        DataManager(this, tracker)
    }
}
