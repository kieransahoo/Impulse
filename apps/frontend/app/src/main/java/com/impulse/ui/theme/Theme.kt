package com.impulse.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ImpulseColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = AccentContainer,
    onPrimaryContainer = PrimaryVariant,
    secondary = Secondary,
    onSecondary = Ink,
    secondaryContainer = Secondary,
    onSecondaryContainer = Ink,
    background = Paper,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = NeutralContainer,
    onSurfaceVariant = Muted,
    error = ErrorColor,
    errorContainer = ErrorContainer,
    outline = Outline
)

@Composable
fun ImpulseTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Paper.toArgb()
            window.navigationBarColor = Surface.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = ImpulseColorScheme,
        typography = Typography,
        shapes = ImpulseShapes,
        content = content
    )
}
