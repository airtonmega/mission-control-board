package com.teseai.live.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = PrimaryDark,
    primaryContainer = PrimaryMedium,
    onPrimaryContainer = OnSurfaceLight,
    secondary = AccentCyan,
    onSecondary = PrimaryDark,
    secondaryContainer = PrimaryLight,
    onSecondaryContainer = OnSurfaceLight,
    tertiary = SuccessGreen,
    background = SurfaceDark,
    onBackground = OnSurfaceLight,
    surface = SurfaceCard,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceCardElevated,
    onSurfaceVariant = OnSurfaceMedium,
    error = ErrorRed,
    outline = OnSurfaceDim,
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnSurfaceLight,
    primaryContainer = AccentBlue,
    onPrimaryContainer = PrimaryDark,
    secondary = AccentCyan,
    onSecondary = PrimaryDark,
    background = OnSurfaceLight,
    onBackground = PrimaryDark,
    surface = OnSurfaceLight,
    onSurface = PrimaryDark,
    error = ErrorRed,
)

@Composable
fun TeseAITheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
