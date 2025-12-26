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
import org.graphstream.graph.implementations.SingleGraph
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

data class Config(
    val speed: Int = 10_000,
)

@ExperimentalCoroutinesApi
suspend fun main() {
    System.setProperty("org.graphstream.ui", "swing");
    val config = MutableStateFlow(Config())
    coroutineScope {
        launch {
            runConfigApp(config, onConfigChange = { config.value = it })
        }
        val graph = SingleGraph("Call tree")
        graph.addNode("root")
        launch {
            graph.display()
        }
        trackingCallStacks {
            highlyBranchingCalls()
        }.collect { (node, event) ->
            config.mapLatest { config ->
                delay(1.seconds / config.speed)
            }.first()
            when (event) {
                CallStackTrackEventType.CallStackPopType -> graph.removeNode("${node.id}")
                is CallStackTrackEventType.CallStackPushType -> {
                    val newId = "${node.id}"
                    val parentId = "${node.parent?.id ?: "root"}"
                    graph.addNode(newId)
                    graph.addEdge("$newId-$parentId", newId, parentId, true)
                }
                is CallStackTrackEventType.CallStackThrowType -> graph.removeNode("${node.id}")
            }
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