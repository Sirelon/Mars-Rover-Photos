package com.sirelon.marsroverphotos

import android.app.Application
import com.crashlytics.android.Crashlytics
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

        Fabric.with(this, Crashlytics())
    }

    val dataManger by lazy {
        DataManager(this)
    }
}