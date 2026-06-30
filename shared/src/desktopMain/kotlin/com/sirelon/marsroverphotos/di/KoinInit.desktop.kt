package com.sirelon.marsroverphotos.di

import android.app.Application
import com.google.firebase.FirebasePlatform
import com.sirelon.marsroverphotos.domain.repositories.RoversRepository
import com.sirelon.marsroverphotos.platform.BuildInfo
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.initialize
import org.koin.core.context.startKoin
import java.util.concurrent.ConcurrentHashMap

fun initKoinDesktop() {
    FirebasePlatform.initializeFirebasePlatform(object : FirebasePlatform() {
        private val storage = ConcurrentHashMap<String, String>()
        override fun store(key: String, value: String) { storage[key] = value }
        override fun retrieve(key: String): String? = storage[key]
        override fun clear(key: String) { storage.remove(key) }
        override fun log(msg: String) = println(msg)
    })
    Firebase.initialize(
        context = Application(),
        options = FirebaseOptions(
            applicationId = "1:947086751944:android:c96a933297fcdc16",
            apiKey = "AIzaSyDQtKGkug6YiqzEUxjjfV-Jh8K4KwIfEgs",
            databaseUrl = "https://mars-rover-photos.firebaseio.com",
            storageBucket = "mars-rover-photos.appspot.com",
            projectId = "mars-rover-photos",
            gcmSenderId = "947086751944"
        )
    )

    BuildInfo.init(
        versionName = System.getProperty("app.version") ?: "unknown",
        isDebug = System.getProperty("app.debug") == "true",
        packageName = "com.sirelon.marsroverphotos"
    )

    val koinApplication = startKoin {
        modules(
            platformModule,
            databaseModule,
            networkModule,
            repositoryModule,
            viewModelModule,
            navigationModule
        )
    }
    koinApplication.koin.get<RoversRepository>().initialize()
}
