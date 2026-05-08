package com.pnzgu.electronix.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class ThemeMode {
    System,
    Light,
    Dark,
}

private val LightScheme = lightColorScheme(
    primary = ElectronixOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE2CC),
    onPrimaryContainer = Color(0xFF442100),
    secondary = ElectronixOrangeLight,
    onSecondary = Color.White,
    tertiary = Color(0xFFB85A00),
    background = ElectronixBackgroundLight,
    onBackground = ElectronixOnLight,
    surface = ElectronixSurfaceLight,
    onSurface = ElectronixOnLight,
    surfaceDim = Color(0xFFD8DCE3),
    surfaceBright = Color(0xFFFFFEFE),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = ElectronixSurfaceContainerLowLight,
    surfaceContainer = ElectronixSurfaceContainerLight,
    surfaceContainerHigh = ElectronixSurfaceContainerHighLight,
    surfaceContainerHighest = ElectronixSurfaceContainerHighestLight,
    surfaceVariant = Color(0xFFDDE0E6),
    onSurfaceVariant = Color(0xFF49454F),
    error = Color(0xFFCD5A00),
    onError = Color.White,
    errorContainer = Color(0xFFFFDBBF),
    onErrorContainer = Color(0xFF3C1F00),
)

private val DarkScheme = darkColorScheme(
    primary = ElectronixAmber,
    onPrimary = Color(0xFF4E2500),
    primaryContainer = ElectronixAmberMuted,
    onPrimaryContainer = Color(0xFFFFDBBF),
    secondary = ElectronixAmber,
    onSecondary = Color(0xFF4E2500),
    tertiary = ElectronixAmberLight,
    background = ElectronixBackgroundDark,
    onBackground = ElectronixOnDark,
    surface = ElectronixSurfaceDark,
    onSurface = ElectronixOnDark,
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    error = Color(0xFFFFA96E),
    onError = Color(0xFF4A1F00),
    errorContainer = Color(0xFF703700),
    onErrorContainer = Color(0xFFFFDBBF),
)

@Composable
fun ElectronixAndroidTheme(
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = Typography,
        content = content,
    )
}
