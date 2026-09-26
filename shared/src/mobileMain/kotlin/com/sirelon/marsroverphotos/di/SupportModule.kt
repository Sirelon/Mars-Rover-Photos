package com.sirelon.marsroverphotos.di

import com.sirelon.marsroverphotos.data.featureflags.FirebaseFeatureFlags
import com.sirelon.marsroverphotos.data.support.RevenueCatSupportRepository
import com.sirelon.marsroverphotos.domain.featureflags.FeatureFlags
import com.sirelon.marsroverphotos.domain.repositories.SupportRepository
import com.sirelon.marsroverphotos.platform.BuildInfo
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Store billing + remote feature flags for Android and iOS.
 *
 * @param revenueCatApiKey the public SDK key for this build: each platform entry point passes its
 *   store key in release and RevenueCat's Test Store key in debug, so the routing is decided by the
 *   build configuration rather than at runtime.
 */
fun supportModule(revenueCatApiKey: String): Module = module {
    single<SupportRepository> {
        RevenueCatSupportRepository(apiKey = revenueCatApiKey, isDebug = BuildInfo.isDebug, scope = get())
    }
    single<FeatureFlags> { FirebaseFeatureFlags(isDebug = BuildInfo.isDebug, scope = get()) }
}
