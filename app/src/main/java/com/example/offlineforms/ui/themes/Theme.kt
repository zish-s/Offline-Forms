package com.example.offlineforms.ui.themes

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

private val LightColorScheme = lightColorScheme(
    primary = Purple,
    onPrimary = White,
    primaryContainer = PurpleLight,
    background = BackgroundLight,
    surface = White,
    onBackground = PurpleDark,
    onSurface = PurpleDark
)

private val DarkColorScheme = darkColorScheme(
    primary = Purple,
    onPrimary = White,
    primaryContainer = PurpleDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    onBackground = White,
    onSurface = White
)

@Composable
fun OfflineFormsTheme(  //s just a wrapper function that says "apply our purple colors, our fonts, our dark/light mode settings to everything inside me." Every screen inside NavGraph() automatically gets those styles without us having to set colors manually on each screen.

    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}