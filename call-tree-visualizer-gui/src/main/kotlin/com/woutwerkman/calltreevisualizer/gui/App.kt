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
import kotlin.time.ExperimentalTime

@ExperimentalCoroutinesApi
suspend fun main() {
    val config = MutableStateFlow(Config())
    runApp(config, onConfigChange = { config.value = it })
}

private suspend fun runApp(currentConfig: Flow<Config>, onConfigChange: (Config) -> Unit) {
    awaitApplication {
        val config by currentConfig.collectAsState(Config())
        var settingsIsOpen by remember { mutableStateOf(false) }
        Window(
            onCloseRequest = ::exitApplication,
            title = "Call Tree Visualizer",
            onKeyEvent = { event ->
                (event.type == KeyEventType.KeyDown && event.isMetaPressed && event.key == Key.Comma)
                    .also { if (it) settingsIsOpen = !settingsIsOpen }
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
                        CallTreeUI(currentConfig, program = { highlyBranchingCalls() })
                    }
                }
            }
        }
    }
}

