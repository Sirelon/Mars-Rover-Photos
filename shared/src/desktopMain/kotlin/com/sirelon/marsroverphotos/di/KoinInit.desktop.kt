package com.sirelon.marsroverphotos.di

import android.app.Application
import com.google.firebase.FirebasePlatform
import com.sirelon.marsroverphotos.domain.repositories.RoversRepository
import com.sirelon.marsroverphotos.platform.BuildInfo
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.initialize
import java.util.concurrent.ConcurrentHashMap
import org.koin.core.module.Module

/**
 * [overrides] replace app bindings before anything resolves them: the store-screenshot renderer
 * swaps in an in-memory database and its own preferences so a render never touches the user's data.
 */
fun initKoinDesktop(overrides: List<Module> = emptyList()) {
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

    val koinApplication = initKoin(listOf(platformModule, supportModule))
    koinApplication.koin.loadModules(overrides, allowOverride = true)
    koinApplication.koin.get<RoversRepository>().initialize()
}
