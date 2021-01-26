package com.sirelon.marsroverphotos.activity

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch


/**
 * Created on 25.01.2021 17:13 for Mars-Rover-Photos.
 */
@Composable
fun TestCompose() {

    val viewModel: TestViewModel = viewModel()

    MaterialTheme {
        val counter by viewModel.stateFlow.collectAsState(initial = 0)
        val timer by viewModel.timerQueue.collectAsState(initial = 0)

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Timer: $timer")
            Spacer(Modifier.preferredHeight(32.dp))
            Text(text = "Counter $counter")
            Spacer(Modifier.preferredHeight(32.dp))
            Button(onClick = { viewModel.incrementCounter() }) {
                Text(text = "Increment counter")
            }
        }
    }
}

@OptIn(FlowPreview::class)
class TestViewModel : ViewModel() {
    private val timeMillis: Long = 3000

    private val counterQueue = MutableStateFlow<Int>(0)
    val timerQueue = MutableStateFlow<Long>(timeMillis)

    private val job = SupervisorJob()

    @OptIn(ExperimentalCoroutinesApi::class)
    val stateFlow = counterQueue
        .transform {
            emit(it)
            delay(1000)
        }
        .transformLatest {
            job.cancelChildren()
            viewModelScope.launch(job) {
//                delay(timeMillis)
                val start = System.currentTimeMillis() + timeMillis
                val delay = async { delay(timeMillis) }
                while (delay.isActive) {
                    val now = System.currentTimeMillis()
                    delay(80)
                    timerQueue.emit(start - now)
                }
                emit(0)
            }.invokeOnCompletion { timerQueue.tryEmit(timeMillis) }
            emit(it)
        }.onEach {
            Log.d("Sirelon", "ON EACH $it")
        }

init {
    viewModelScope.launch {
        stateFlow.collect {
            Log.d("Sirelon", "COLLECT $it")
//                counterQueue.emit(it+1)
        }
    }
}
    fun incrementCounter() {

        viewModelScope.launch {
//            Log.d("Sirelon", "COLLECT ${stateFlow.toList()}")
            val counter = stateFlow.first() + 1
            counterQueue.emit(counter)
//            stateFlow.collect {
//                Log.d("Sirelon", "COLLECT $it")
////                counterQueue.emit(it+1)
//            }

        }
    }

}


