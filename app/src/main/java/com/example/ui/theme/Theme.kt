package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ScratchLightColorScheme = lightColorScheme(
    primary = ScratchBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F1FF),
    onPrimaryContainer = ScratchBlueDark,
    secondary = ScratchOrange,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFF4E5),
    onSecondaryContainer = Color(0xFF8A5300),
    background = Color(0xFFF9F9F9),
    onBackground = Color(0xFF2E384D),
    surface = Color.White,
    onSurface = Color(0xFF2E384D),
    surfaceVariant = Color(0xFFEDF2F9),
    onSurfaceVariant = Color(0xFF576075),
    error = ScratchRed,
    onError = Color.White,
    errorContainer = ErrorContainer,
    onErrorContainer = Color(0xFF8B0000)
)

private val ScratchDarkColorScheme = darkColorScheme(
    primary = ScratchBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A5F),
    onPrimaryContainer = Color(0xFFBCE0FD),
    secondary = ScratchOrange,
    onSecondary = Color.White,
    background = Color(0xFF1B2230),
    onBackground = Color(0xFFE8EDF5),
    surface = Color(0xFF242C3D),
    onSurface = Color(0xFFE8EDF5),
    surfaceVariant = Color(0xFF2F384C),
    onSurfaceVariant = Color(0xFFB3BAC7),
    error = ScratchRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) ScratchDarkColorScheme else ScratchLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
