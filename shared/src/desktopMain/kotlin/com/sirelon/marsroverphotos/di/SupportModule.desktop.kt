package com.sirelon.marsroverphotos.di

import com.sirelon.marsroverphotos.data.featureflags.StaticFeatureFlags
import com.sirelon.marsroverphotos.data.support.FakeSupportRepository
import com.sirelon.marsroverphotos.domain.featureflags.FeatureFlag
import com.sirelon.marsroverphotos.domain.featureflags.FeatureFlags
import com.sirelon.marsroverphotos.domain.repositories.SupportRepository
import org.koin.dsl.module

/**
 * Desktop has neither a store nor Remote Config (GitLive's JVM shim ships it non-functional), so
 * billing is faked and the Support feature is simply on — desktop is where the screen gets iterated.
 */
val supportModule = module {
    single<SupportRepository> { FakeSupportRepository() }
    single<FeatureFlags> { StaticFeatureFlags(overrides = mapOf(FeatureFlag.SUPPORT_TIERS to true)) }
}
