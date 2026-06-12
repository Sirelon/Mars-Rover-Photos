package com.sirelon.marsroverphotos.di

import com.sirelon.marsroverphotos.domain.repositories.RoversRepository
import com.sirelon.marsroverphotos.platform.BuildInfo
import org.koin.core.context.startKoin
import platform.Foundation.NSBundle

object IosApp {
    fun start(isDebug: Boolean) {
        BuildInfo.init(
            versionName = NSBundle.mainBundle.infoDictionary
                ?.get("CFBundleShortVersionString") as? String ?: "unknown",
            isDebug = isDebug,
            packageName = NSBundle.mainBundle.bundleIdentifier ?: "com.sirelon.marsroverphotos"
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
}
