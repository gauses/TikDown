package com.donut.tikdown.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

val colorScheme = lightColorScheme(
    primary = Color(0xFF24a0ed),
    secondary = Color(0xFF625b71),
    tertiary = Color(0xFF7D5260),
//    tertiaryContainer = Color(0xFFF0004E),
//    primaryContainer = Color(0xFFF0004E),
    secondaryContainer = Color(0x3662B5E8),
    background = Color(0xFFE6DFEB),
    surface = Color(0xFFE6DFEB),
    outline = Color(0xA624A0ED),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
)

@Composable
fun MainTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {


    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}