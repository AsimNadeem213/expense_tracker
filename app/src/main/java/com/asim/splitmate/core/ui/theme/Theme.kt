package com.asim.splitmate.core.ui.theme

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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = SurfaceLight,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldDark,
    secondary = IndigoAccent,
    tertiary = AmberAccent,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = androidx.compose.ui.graphics.Color(0xFFCBD5E1),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFFE2E8F0),
    error = androidx.compose.ui.graphics.Color(0xFFDC2626),
    onError = androidx.compose.ui.graphics.Color.White,
    errorContainer = androidx.compose.ui.graphics.Color(0xFFFEE2E2),
    onErrorContainer = androidx.compose.ui.graphics.Color(0xFF991B1B)
)

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldLight,
    onPrimary = androidx.compose.ui.graphics.Color(0xFF042F2C),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF115E59),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFCCFBF1),
    secondary = androidx.compose.ui.graphics.Color(0xFF818CF8),
    onSecondary = androidx.compose.ui.graphics.Color(0xFF1E1B4B),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF312E81),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFE0E7FF),
    tertiary = androidx.compose.ui.graphics.Color(0xFFFBBF24),
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = androidx.compose.ui.graphics.Color(0xFF475569),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFF334155),
    error = androidx.compose.ui.graphics.Color(0xFFF87171),
    onError = androidx.compose.ui.graphics.Color(0xFF450A0A),
    errorContainer = androidx.compose.ui.graphics.Color(0xFF7F1D1D),
    onErrorContainer = androidx.compose.ui.graphics.Color(0xFFFECACA)
)

@Composable
fun ExpenseMateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
