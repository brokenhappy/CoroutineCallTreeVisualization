package com.woutwerkman.calltreevisualizer.gui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Checkbox
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import kotlin.math.roundToInt

data class Config(val speed: Int? = DefaultSpeed)

private const val DefaultSpeed = 3

@Composable
internal fun Settings(currentConfig: Config, onConfigChange: (Config) -> Unit) {
    Column {
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
    }
}