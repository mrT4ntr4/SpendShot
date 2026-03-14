package com.spendshot.android.ui.theme

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
import com.spendshot.android.data.Theme
import androidx.compose.ui.graphics.Color


private val DarkColorPalette = darkColorScheme(
    primary = AccentBlue,               // NOT green
    onPrimary = Color.White,

    secondary = AccentGreen,             // Green = accent only
    onSecondary = Color.Black,

    tertiary = AccentAmber,
    onTertiary = Color.Black,

    background = DarkBackground,
    onBackground = DarkOnBackground,

    surface = DarkSurface,
    onSurface = DarkOnSurface,

    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,

    outline = DarkOutline,

    error = AccentRed,
    onError = Color.Black
)

private val LightColorPalette = lightColorScheme(
    primary = AccentBlue,              // Neutral primary
    onPrimary = Color.White,

    secondary = AccentGreen,            // Money accent
    onSecondary = Color.White,

    tertiary = AccentAmber,
    onTertiary = Color.Black,

    background = LightBackground,
    onBackground = LightOnBackground,

    surface = LightSurface,
    onSurface = LightOnSurface,

    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,

    outline = LightOutline,

    error = AccentRed,
    onError = Color.White
)

@Composable
fun SpendShotTheme(
    appTheme: Theme? = null,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (appTheme) {
        Theme.DARK -> true
        Theme.LIGHT -> false
        null -> darkTheme
    }
    val colors = if (useDarkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colors.background.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = colors.surfaceVariant.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDarkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !useDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}