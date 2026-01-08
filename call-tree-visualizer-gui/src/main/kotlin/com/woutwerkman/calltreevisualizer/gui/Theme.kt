package com.woutwerkman.calltreevisualizer.gui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DarkColorPalette = darkColors(
    primary = Color(0xFFD0BCFF),
    primaryVariant = Color(0xFF381E72),
    secondary = Color(0xFFCCC2DC),
    surface = Color(0xFF322F37), // Slightly lighter for better contrast
    onSurface = Color(0xFFE6E1E5),
    background = Color(0xFF141218), // Slightly darker for better contrast
    onBackground = Color(0xFFE6E1E5),
    error = Color(0xFFF2B8B5)
)

private val LightColorPalette = lightColors(
    primary = Color(0xFF6750A4),
    primaryVariant = Color(0xFF3700B3),
    secondary = Color(0xFF625B71),
    surface = Color(0xFFEADDFF), // More distinct lavender
    onSurface = Color(0xFF1C1B1F),
    background = Color(0xFFFFF7FF), // Brighter background
    onBackground = Color(0xFF1C1B1F),
    error = Color(0xFFB3261E)
)

val Typography = Typography(
    h6 = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    ),
    subtitle1 = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp
    ),
    body1 = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    ),
    body2 = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp
    ),
    caption = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        color = Color.Gray
    )
)

@Composable
fun CallTreeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        content = content
    )
}
