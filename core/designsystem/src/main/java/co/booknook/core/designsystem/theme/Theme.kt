package co.booknook.core.designsystem.theme

import android.app.Activity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
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
    background = Charcoal,           // 0xFF0F0F0F — true black bg
    surface = DeepCharcoal,          // 0xFF1C1C1E — elevated cards
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFF0EAE0), // warm off-white text on dark
    onSurface = Color(0xFFB0A898)     // muted warm grey for secondary text
)

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalNavAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

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
