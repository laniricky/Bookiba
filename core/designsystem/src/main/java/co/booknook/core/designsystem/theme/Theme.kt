package co.booknook.core.designsystem.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Accent,
    secondary = Secondary,
    background = Cream,
    surface = SurfaceColor,
    onPrimary = SurfaceColor,
    onSecondary = SurfaceColor,
    onBackground = OnSurfaceColor,
    onSurface = OnSurfaceColor
)

private val DarkColorScheme = darkColorScheme(
    primary = Accent,
    secondary = Secondary,
    background = OnSurfaceColor,
    surface = Color(0xFF2C2C2C),
    onPrimary = SurfaceColor,
    onSecondary = SurfaceColor,
    onBackground = Cream,
    onSurface = Cream
)

@Composable
fun BookibaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
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
        typography = BookibaTypography,
        content = content
    )
}
