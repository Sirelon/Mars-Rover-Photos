package com.sirelon.marsroverphotos.domain.featureflags

import kotlinx.coroutines.flow.StateFlow

interface FeatureFlags {
    /**
     * Current value of [flag], starting at [FeatureFlag.default] and updating when a remote value
     * is activated. Never throws: a failed fetch leaves the default in place.
     */
    fun isEnabled(flag: FeatureFlag): StateFlow<Boolean>
}
