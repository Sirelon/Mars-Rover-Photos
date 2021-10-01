package com.sirelon.marsroverphotos.feature.billing

import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull

/**
 * Created on 01.10.2021 23:45 for Mars-Rover-Photos.
 */
class AdvertisementConfigurator(private val activity: FragmentActivity) {

    private val adEnabledMutableFlow = MutableStateFlow<Boolean?>(null)
    val adEnabledFlow = adEnabledMutableFlow.filterNotNull()

    init {
        adEnabledMutableFlow.value = true
    }

}