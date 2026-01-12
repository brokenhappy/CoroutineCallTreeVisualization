package com.woutwerkman.calltreevisualizer.gui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

data class Config(
    val speed: Int? = DefaultSpeed,
    val zoom: Float = 1f,
    val themeMode: ThemeMode = ThemeMode.System,
)

enum class ThemeMode {
    System, Light, Dark
}

private const val DefaultSpeed = 3

@Composable
internal fun Settings(currentConfig: Config, onConfigChange: (Config) -> Unit) {
    Card(
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = MaterialTheme.colors.surface,
        contentColor = MaterialTheme.colors.onSurface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Settings",
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(currentConfig.speed != null, onCheckedChange = {
                    onConfigChange(currentConfig.copy(speed = if (it) DefaultSpeed else null))
                })
                Text("Slow down execution", style = MaterialTheme.typography.body1)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("Events per second: ${currentConfig.speed ?: "-"}", style = MaterialTheme.typography.caption)
            Slider(
                value = (currentConfig.speed ?: DefaultSpeed).toFloat(),
                enabled = currentConfig.speed != null,
                valueRange = 0.0f..500.0f,
                onValueChange = { onConfigChange(currentConfig.copy(speed = it.roundToInt())) },
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colors.primary,
                    activeTrackColor = MaterialTheme.colors.primary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Text("Zoom: ", style = MaterialTheme.typography.caption)
                var textFieldState by remember(currentConfig.zoom) { mutableStateOf(currentConfig.zoom.times(100).roundToInt()) }
                TextField(
                    value = textFieldState.toString(),
                    onValueChange = { newValueString ->
                        newValueString.toIntOrNull()?.also { textFieldState = it }
                    },
                    modifier = Modifier.onKeyEvent { event ->
                        (event.type == KeyEventType.KeyUp && event.key == Key.Enter).also {
                            if (it) onConfigChange(currentConfig.copy(zoom = textFieldState.div(100f)))
                        }
                    },
                    colors = TextFieldDefaults.textFieldColors(

                    ),
                )
                Text("%", style = MaterialTheme.typography.caption)
            }
            Slider(
                value = currentConfig.zoom,
                valueRange = 0.1f..8.0f,
                onValueChange = { onConfigChange(currentConfig.copy(zoom = it)) },
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colors.primary,
                    activeTrackColor = MaterialTheme.colors.primary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Theme", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
            Row {
                ThemeMode.entries.forEach { mode ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(
                            selected = currentConfig.themeMode == mode,
                            onClick = { onConfigChange(currentConfig.copy(themeMode = mode)) },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colors.primary)
                        )
                        Text(mode.name, style = MaterialTheme.typography.body2)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }
        }
    }
}