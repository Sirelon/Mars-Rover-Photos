package com.sirelon.marsroverphotos.platform

import kotlinx.coroutines.flow.MutableStateFlow

object AndroidAdConsent {
    val canRequestAds = MutableStateFlow(false)
    val personalizedAds = MutableStateFlow(false)
}
