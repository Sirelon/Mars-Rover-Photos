package com.sirelon.marsroverphotos.tracker

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Created on 25.07.2021 21:22 for Mars-Rover-Photos.
 */
class DataFlowCollector<T>(
    collectTime: Long = 1200,
    scope: CoroutineScope = GlobalScope,
    callback: (first: T?, last: T?) -> Unit
) {

    private val flow = MutableStateFlow<T?>(null)

    init {
        scope.launch {
            var firstValue: T? = null

            flow
                .onEach {
                    if (firstValue == null && it != null) {
                        firstValue = it
                    }
                }
                .debounce(collectTime)
                .collectLatest {
                    callback(firstValue, it)
                    firstValue = null
                }
        }
    }


    fun onEvent(data: T) {
        flow.value = data
    }

}