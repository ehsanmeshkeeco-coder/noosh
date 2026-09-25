package com.example.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = NooshPrimary,
    onPrimary = Color.White,
    primaryContainer = NooshSubtleBlue,
    onPrimaryContainer = NooshPrimaryDark,
    secondary = NooshPrimaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0EEFF),
    onSecondaryContainer = NooshPrimaryDark,
    tertiary = NooshAccent,
    background = Color(0xFFE8F6FF),
    onBackground = TextPrimary,
    surface = Color.White,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFDDF2FF),
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFFBAE6FD)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF082F49),
    primaryContainer = Color(0xFF0369A1),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = Color(0xFF7DD3FC),
    onSecondary = Color(0xFF0C4A6E),
    secondaryContainer = Color(0xFF1E293B),
    onSecondaryContainer = Color(0xFFBAE6FD),
    tertiary = Color(0xFF34D399),
    onTertiary = Color(0xFF064E3B),
    background = Color(0xFF0B132B),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF162038),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155)
)

@Composable
fun NooshTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    // Explicitly enforce Right-To-Left direction for Persian
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
