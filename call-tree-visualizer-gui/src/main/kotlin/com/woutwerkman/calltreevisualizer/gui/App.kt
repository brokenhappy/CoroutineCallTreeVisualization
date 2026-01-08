@file:OptIn(ExperimentalTime::class)

package com.woutwerkman.calltreevisualizer.gui

import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.awaitApplication
import com.woutwerkman.calltreevisualizer.highlyBranchingCalls
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlin.time.ExperimentalTime

@ExperimentalCoroutinesApi
suspend fun main() {
    val config = MutableStateFlow(Config())
    val manualStepSignals = Channel<Unit>(Channel.CONFLATED)
    runApp(config, manualStepSignals, onConfigChange = { config.value = it })
}

private suspend fun runApp(
    currentConfig: Flow<Config>,
    manualStepSignals: Channel<Unit>,
    onConfigChange: (Config) -> Unit
) {
    awaitApplication {
        val config by currentConfig.collectAsState(Config())
        var settingsIsOpen by remember { mutableStateOf(false) }
        Window(
            onCloseRequest = ::exitApplication,
            title = "Call Tree Visualizer",
            onKeyEvent = { event ->
                if (event.type == KeyEventType.KeyDown && event.isMetaPressed && event.key == Key.Comma) {
                    settingsIsOpen = !settingsIsOpen
                    true
                } else if (event.type == KeyEventType.KeyDown && event.key == Key.F8) {
                    manualStepSignals.trySend(Unit)
                    true
                } else {
                    false
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
                        CallTreeUI(currentConfig, manualStepSignals.receiveAsFlow(), program = { highlyBranchingCalls() })
                    }
                }
            }
        }
    }
}

