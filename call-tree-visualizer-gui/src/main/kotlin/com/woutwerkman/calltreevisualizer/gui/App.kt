package com.woutwerkman.calltreevisualizer.gui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.awaitApplication
import com.woutwerkman.calltreevisualizer.coroutineintegration.CallStackTrackEventType
import com.woutwerkman.calltreevisualizer.coroutineintegration.trackingCallStacks
import com.woutwerkman.calltreevisualizer.highlyBranchingCalls
import com.woutwerkman.calltreevisualizer.measureLinearly
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

data class Config(
    val speed: Int = 10_000,
)

@ExperimentalCoroutinesApi
suspend fun main() {
    val config = MutableStateFlow(Config())
    coroutineScope {
        launch {
            runConfigApp(config, onConfigChange = { config.value = it })
        }
        trackingCallStacks {
            highlyBranchingCalls()
        }.collect { (node, event) ->
            config.mapLatest { config ->
                delay(1.seconds / config.speed)
            }.first()
        }
    }
}

private suspend fun runConfigApp(currentConfig: Flow<Config>, onConfigChange: (Config) -> Unit) {
    awaitApplication {
        val currentConfig by currentConfig.collectAsState(Config())
        Window(onCloseRequest = ::exitApplication, title = "Call Tree Visualizer Config") {
            Column {
                Text("Events per second: ${currentConfig.speed}")
                Slider(
                    value = currentConfig.speed.toFloat(),
                    valueRange = 1.0f..10_000.0f,
                    onValueChange = { onConfigChange(currentConfig.copy(speed = it.roundToInt())) },
                )
            }
        }
    }
}