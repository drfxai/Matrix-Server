package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = MatrixGreen,
    onPrimary = MatrixBlack,
    primaryContainer = MatrixSurfaceVariant,
    onPrimaryContainer = MatrixGreen,
    secondary = MatrixGreenDark,
    onSecondary = MatrixBlack,
    secondaryContainer = MatrixDarkSurface,
    onSecondaryContainer = MatrixGreen,
    tertiary = MatrixBlue,
    onTertiary = MatrixBlack,
    background = MatrixBlack,
    onBackground = MatrixTextPrimary,
    surface = MatrixDarkSurface,
    onSurface = MatrixTextPrimary,
    surfaceVariant = MatrixSurfaceVariant,
    onSurfaceVariant = MatrixTextSecondary,
    outline = MatrixSurfaceBorder,
    error = MatrixRed,
    onError = MatrixBlack
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Matrix theme is fundamentally a dark, cyber aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = MatrixBlack.toArgb()
                window.navigationBarColor = MatrixBlack.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
