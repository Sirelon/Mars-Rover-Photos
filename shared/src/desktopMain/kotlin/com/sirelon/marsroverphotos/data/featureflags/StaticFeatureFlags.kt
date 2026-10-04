package com.sirelon.marsroverphotos.data.featureflags

import com.sirelon.marsroverphotos.domain.featureflags.FeatureFlag
import com.sirelon.marsroverphotos.domain.featureflags.FeatureFlags
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Fixed flags for a platform without Remote Config; [overrides] win over [FeatureFlag.default]. */
class StaticFeatureFlags(overrides: Map<FeatureFlag, Boolean> = emptyMap()) : FeatureFlags {
    private val flows = FeatureFlag.entries.associateWith { MutableStateFlow(overrides[it] ?: it.default) }
    override fun isEnabled(flag: FeatureFlag): StateFlow<Boolean> = flows.getValue(flag)
}
