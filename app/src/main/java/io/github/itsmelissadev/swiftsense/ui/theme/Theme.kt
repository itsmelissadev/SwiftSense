package io.github.itsmelissadev.swiftsense.ui.theme

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkSecondary,
    onPrimaryContainer = DarkForeground,
    background = DarkBackground,
    onBackground = DarkForeground,
    surface = DarkCard,
    onSurface = DarkCardForeground,
    surfaceVariant = DarkMuted,
    onSurfaceVariant = DarkOnMuted,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkMuted,
    onSecondaryContainer = DarkForeground,
    tertiary = DarkAccent,
    onTertiary = DarkOnAccent,
    outline = DarkBorder,
    outlineVariant = DarkBorder.copy(alpha = 0.5f),
    error = Error
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightSecondary,
    onPrimaryContainer = LightForeground,
    background = LightBackground,
    onBackground = LightForeground,
    surface = LightCard,
    onSurface = LightCardForeground,
    surfaceVariant = LightMuted,
    onSurfaceVariant = LightOnMuted,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightMuted,
    onSecondaryContainer = LightForeground,
    tertiary = LightAccent,
    onTertiary = LightOnAccent,
    outline = LightBorder,
    outlineVariant = LightBorder.copy(alpha = 0.5f),
    error = Error
)


@Composable
fun SwiftSenseTheme(
    themeMode: String = "system",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

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
            val context = view.context
            if (context is Activity) {
                val window = context.window
                val insetsController = WindowCompat.getInsetsController(window, view)

                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
