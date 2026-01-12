@file:OptIn(ExperimentalTime::class)

package com.woutwerkman.calltreevisualizer.gui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.awaitApplication
import com.woutwerkman.calltreevisualizer.baz
import com.woutwerkman.calltreevisualizer.firstStructuredConcurrency
import com.woutwerkman.calltreevisualizer.linearExplosion
import com.woutwerkman.calltreevisualizer.programWithAllTypes
import com.woutwerkman.calltreevisualizer.recurse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime

@ExperimentalCoroutinesApi
suspend fun main() {
    val breakpointProgram = changeSpeed(30.eventsPerSecond)
        .then(breakAfter(functionCall(::linearExplosion)))
        .then(changeSpeed(30.eventsPerSecond))
        .then(breakBefore(functionThrows(::recurse)))
        .then(breakAfter(functionCall(::firstStructuredConcurrency)))
        .then(changeSpeed(10.eventsPerSecond))
        .then(breakAfter(functionCall("kotlinhax.shadowroutines.coroutineScope")))
        .then(changeSpeed(10.eventsPerSecond))
        .then(breakBefore(functionThrows(::baz)))
        .then(changeSpeed(10.eventsPerSecond))
        .then(breakBefore(functionCancels("kotlinhax.shadowroutines.awaitCancellation")))

    val config = MutableStateFlow(Config())
    val stepSignals = MutableSharedFlow<StepSignal>(replay = 10)
    runApp(
        currentConfig = config,
        stepSignals = stepSignals,
        breakpointProgram = breakpointProgram,
        onConfigChange = { config.value = it },
        program = { programWithAllTypes() },
    )
}

private suspend fun runApp(
    currentConfig: Flow<Config>,
    stepSignals: MutableSharedFlow<StepSignal>,
    breakpointProgram: BreakpointProgram,
    onConfigChange: (Config) -> Unit,
    program: suspend () -> Unit,
) {
    awaitApplication {
        val config by currentConfig.collectAsState(Config())
        var settingsIsOpen by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        Window(
            onCloseRequest = ::exitApplication,
            title = "Call Tree Visualizer",
            onKeyEvent = { event ->
                event.type == KeyEventType.KeyDown && when {
                    event.isMetaPressed && event.key == Key.Comma -> {
                        settingsIsOpen = !settingsIsOpen
                        true
                    }
                    event.key == Key.F8 -> {
                        scope.launch { stepSignals.emit(StepSignal.Step) }
                        true
                    }
                    event.key == Key.F9 || event.key == Key.R -> {
                        scope.launch { stepSignals.emit(StepSignal.Resume) }
                        true
                    }
                    else -> false
                }
            },
        ) {
            MenuBar {
                Menu("Help") {
                    Item(
                        "${if (settingsIsOpen) "Close" else "Open"} Settings",
                        onClick = { settingsIsOpen = !settingsIsOpen },
                    )
                }
            }
            AppUi(config, onConfigChange, settingsIsOpen, currentConfig, stepSignals, breakpointProgram, program)
        }
    }
}

@Composable
private fun AppUi(
    config: Config,
    onConfigChange: (Config) -> Unit,
    settingsIsOpen: Boolean,
    currentConfig: Flow<Config>,
    stepSignals: MutableSharedFlow<StepSignal>,
    breakpointProgram: BreakpointProgram,
    program: suspend () -> Unit,
) {
    val darkTheme = when (config.themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    CallTreeTheme(darkTheme = darkTheme) {
        CompositionLocalProvider(
            LocalDensity provides Density(LocalDensity.current.density * config.zoom)
        ) {
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
                            program = program,
                        )
                    }
                }
            }
        }
    }
}

