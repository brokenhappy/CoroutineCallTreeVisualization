package com.woutwerkman.calltreevisualizer.gui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

data class Config(
    val speed: Int? = DefaultSpeed,
    val themeMode: ThemeMode = ThemeMode.System,
)

enum class ThemeMode {
    System, Light, Dark
}

private const val DefaultSpeed = 3

@Composable
internal fun Settings(currentConfig: Config, onConfigChange: (Config) -> Unit) {
    Surface(
        color = MaterialTheme.colors.background,
        contentColor = MaterialTheme.colors.onBackground
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row {
                Text("Slowing down program:")
                Checkbox(currentConfig.speed != null, onCheckedChange = {
                    onConfigChange(currentConfig.copy(speed = if (it) DefaultSpeed else null))
                })
            }
            Text("Events per second: ${currentConfig.speed ?: "-"}")
            Slider(
                value = (currentConfig.speed ?: DefaultSpeed).toFloat(),
                enabled = currentConfig.speed != null,
                valueRange = 0.0f..500.0f,
                onValueChange = { onConfigChange(currentConfig.copy(speed = it.roundToInt())) },
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Theme:")
            Row {
                ThemeMode.entries.forEach { mode ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(
                            selected = currentConfig.themeMode == mode,
                            onClick = { onConfigChange(currentConfig.copy(themeMode = mode)) }
                        )
                        Text(mode.name)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }
        }
    }
}