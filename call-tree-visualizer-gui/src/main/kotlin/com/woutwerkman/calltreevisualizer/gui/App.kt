@file:OptIn(ExperimentalTime::class)

package com.woutwerkman.calltreevisualizer.gui

import com.woutwerkman.calltreevisualizer.gui.EventsPerSecond.Companion.eventsPerSecond
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
import com.woutwerkman.calltreevisualizer.linearExplosion
import com.woutwerkman.calltreevisualizer.recurse
import com.woutwerkman.calltreevisualizer.testProgram
import com.woutwerkman.calltreevisualizer.coroutineintegration.CallStackTrackEvent
import com.woutwerkman.calltreevisualizer.coroutineintegration.trackingCallStacks
import com.woutwerkman.calltreevisualizer.owningGlobalScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import kotlin.time.ExperimentalTime

@ExperimentalCoroutinesApi
fun main() = runDebugger(
    events = trackingCallStacks {
        owningGlobalScope {
            testProgram()
        }
    },
    breakpointProgram = changeSpeed(30.eventsPerSecond)
        .then(breakAfter(functionCall(::linearExplosion)))
        .then(changeSpeed(30.eventsPerSecond))
        .then(breakBefore(functionThrows(::recurse)))
        .then(changeSpeed(10.eventsPerSecond))
        .then(breakAfter(functionCall("kotlinhax.shadowroutines.coroutineScope")))
        .then(changeSpeed(10.eventsPerSecond))
        .then(breakBefore(functionThrows(::baz)))
        .then(changeSpeed(10.eventsPerSecond))
        .then(breakBefore(functionCancels("kotlinhax.shadowroutines.awaitCancellation"))),
)

@OptIn(ExperimentalCoroutinesApi::class)
fun runDebugger(events: Flow<CallStackTrackEvent>, breakpointProgram: BreakpointProgram) {
    val config = MutableStateFlow(Config())
    val stepSignals = MutableSharedFlow<StepSignal>(replay = 10)
    runBlocking {
        if (events.firstOrNull() == null) error("The program is empty!")
        runApp(
            events = events,
            breakpointProgram = breakpointProgram,
            currentConfig = config,
            onConfigChange = { config.value = it },
            stepSignals = stepSignals,
            onStepSignal = { stepSignals.tryEmit(it) },
        )
    }
}

private suspend fun runApp(
    events: Flow<CallStackTrackEvent>,
    breakpointProgram: BreakpointProgram,
    currentConfig: Flow<Config>,
    onConfigChange: (Config) -> Unit,
    stepSignals: Flow<StepSignal>,
    onStepSignal: (StepSignal) -> Unit,
) {
    awaitApplication {
        val config by currentConfig.collectAsState(Config())
        var settingsIsOpen by remember { mutableStateOf(false) }
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
                        onStepSignal(StepSignal.Step)
                        true
                    }
                    event.key == Key.F9 || event.key == Key.R -> {
                        onStepSignal(StepSignal.Resume)
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
            AppUi(
                events = events,
                breakpointProgram = breakpointProgram,
                settingsIsOpen = settingsIsOpen,
                config = config,
                currentConfig = currentConfig,
                onConfigChange = onConfigChange,
                stepSignals = stepSignals,
                onStepSignal = onStepSignal,
            )
        }
    }
}

@Composable
private fun AppUi(
    events: Flow<CallStackTrackEvent>,
    breakpointProgram: BreakpointProgram,
    settingsIsOpen: Boolean,
    config: Config,
    currentConfig: Flow<Config>,
    onConfigChange: (Config) -> Unit,
    stepSignals: Flow<StepSignal>,
    onStepSignal: (StepSignal) -> Unit,
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
                            events = events,
                            breakpointProgram = breakpointProgram,
                            config = currentConfig,
                            onConfigChange = onConfigChange,
                            stepSignals = stepSignals,
                            onStepSignal = onStepSignal,
                        )
                    }
                }
            }
        }
    }
}
