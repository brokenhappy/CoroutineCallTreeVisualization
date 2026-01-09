@file:OptIn(ExperimentalTime::class)

package com.woutwerkman.calltreevisualizer.gui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.awaitApplication
import com.woutwerkman.calltreevisualizer.foobs
import com.woutwerkman.calltreevisualizer.highlyBranchingCalls
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime

@ExperimentalCoroutinesApi
suspend fun main() {
    val breakpointProgram = changeSpeed(10.eventsPerSecond)
        .then(breakAfter(functionCall(::highlyBranchingCalls)))
        .then(changeSpeed(10.eventsPerSecond))
        .then(breakBefore(functionThrows(::foobs)))
        .then(changeSpeed(10.eventsPerSecond))
        .then(breakBefore(functionCancels("kotlinhax.shadowroutines.awaitCancellation")))

    val config = MutableStateFlow(Config())
    val stepSignals = MutableSharedFlow<StepSignal>(replay = 10)
    runApp(config, stepSignals, breakpointProgram, onConfigChange = { config.value = it })
}

private suspend fun runApp(
    currentConfig: Flow<Config>,
    stepSignals: MutableSharedFlow<StepSignal>,
    breakpointProgram: BreakpointProgram,
    onConfigChange: (Config) -> Unit
) {
    awaitApplication {
        val config by currentConfig.collectAsState(Config())
        var settingsIsOpen by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        Window(
            onCloseRequest = ::exitApplication,
            title = "Call Tree Visualizer",
            onKeyEvent = { event ->
                when (event.type) {
                    KeyEventType.KeyDown if event.isMetaPressed && event.key == Key.Comma -> {
                        settingsIsOpen = !settingsIsOpen
                        true
                    }
                    KeyEventType.KeyDown if event.key == Key.F8 -> {
                        scope.launch { stepSignals.emit(StepSignal.Step) }
                        true
                    }
                    KeyEventType.KeyDown if event.key == Key.F9 || event.key == Key.R -> {
                        scope.launch { stepSignals.emit(StepSignal.Resume) }
                        true
                    }
                    else -> false
                }
            },
        ) {
            val darkTheme = when (config.themeMode) {
                ThemeMode.System -> androidx.compose.foundation.isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            CallTreeTheme(darkTheme = darkTheme) {
                MenuBar {
                    Menu("Help") {
                        Item("${if (settingsIsOpen) "Close" else "Open"} Settings", onClick = { settingsIsOpen = !settingsIsOpen })
                    }
                }
                Surface(color = MaterialTheme.colors.background) {
                    Column {
                        if (settingsIsOpen) {
                            Settings(config, onConfigChange)
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            CallTreeUI(
                                config = currentConfig,
                                stepSignals = stepSignals,
                                breakpointProgram = breakpointProgram,
                                onConfigChange = onConfigChange,
                                program = { highlyBranchingCalls() }
                            )
                        }
                    }
                }
            }
        }
    }
}

