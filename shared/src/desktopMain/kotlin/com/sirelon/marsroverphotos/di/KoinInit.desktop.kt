package com.sirelon.marsroverphotos.di

import com.sirelon.marsroverphotos.domain.repositories.RoversRepository
import com.sirelon.marsroverphotos.platform.BuildInfo
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.initialize
import org.koin.core.context.startKoin

/**
 * Initialize Koin for Desktop (JVM).
 * Called from the desktop app's main function.
 */
fun initKoinDesktop() {
    val platform = object : com.google.firebase.FirebasePlatform() {
        private val storage = mutableMapOf<String, String>()
        override fun store(key: String, value: String) { storage[key] = value }
        override fun retrieve(key: String): String? = storage[key]
        override fun clear(key: String) { storage.remove(key) }
        override fun log(msg: String) { println("[Firebase] $msg") }
    }
    com.google.firebase.FirebasePlatform::class.java
        .getDeclaredField("firebasePlatform")
        .apply { isAccessible = true }
        .set(null, platform)

    Firebase.initialize(
        context = android.app.Application(),
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
            platformModule,      // Desktop-specific dependencies
            databaseModule,      // Room database and DAOs
            networkModule,       // Ktor and REST API
            repositoryModule,    // Repository implementations
            viewModelModule,     // ViewModels
            navigationModule     // Navigation 3 entries
        )
    }
    koinApplication.koin.get<RoversRepository>().initialize()
}
