package com.reelspot.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ReelSpot Custom Colors
val PrimaryGradientPink = Color(0xFFE1306C)
val PrimaryPurple = Color(0xFF833AB4)
val VibrantOrange = Color(0xFFF77737)
val SoftBackground = Color(0xFFF8F9FA)
val DarkBackground = Color(0xFF121212)
val CardSurfaceLight = Color(0xFFFFFFFF)
val CardSurfaceDark = Color(0xFF1E1E1E)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGradientPink,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E2),
    onPrimaryContainer = Color(0xFF3E001D),
    secondary = PrimaryPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E5F5),
    onSecondaryContainer = Color(0xFF2B0038),
    tertiary = VibrantOrange,
    background = SoftBackground,
    surface = CardSurfaceLight,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB0C8),
    onPrimary = Color(0xFF5E002A),
    primaryContainer = Color(0xFF83003E),
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = Color(0xFFE1BEE7),
    onSecondary = Color(0xFF4A0060),
    secondaryContainer = Color(0xFF6A008A),
    onSecondaryContainer = Color(0xFFF3E5F5),
    tertiary = VibrantOrange,
    background = DarkBackground,
    surface = CardSurfaceDark,
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5)
)

@Composable
fun ReelspotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

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
        content = content
    )
}
