package com.vybe.music.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = AmoledBlack,
    primaryContainer = FrostedDarkSurfaceElevated,
    onPrimaryContainer = PureWhite,
    secondary = TextSecondary,
    onSecondary = PureWhite,
    background = AmoledBlack,
    onBackground = PureWhite,
    surface = AmoledBlack,
    onSurface = PureWhite,
    surfaceVariant = FrostedDarkSurface,
    onSurfaceVariant = TextSecondary,
    outline = TranslucentGlassHighlight,
    surfaceTint = androidx.compose.ui.graphics.Color.Transparent
)

@Composable
fun VybeTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            try {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // App-Wide Reference-Width Density Scaling (390dp reference width)
    val baseDensity = LocalDensity.current
    val configuration = LocalConfiguration.current
    val displayMetrics = LocalContext.current.resources.displayMetrics
    val targetDensity = if (displayMetrics.widthPixels < displayMetrics.heightPixels) {
        (displayMetrics.widthPixels / 390f).coerceIn(baseDensity.density * 0.85f, baseDensity.density * 1.25f)
    } else {
        baseDensity.density
    }
    val scaledDensity = Density(density = targetDensity, fontScale = baseDensity.fontScale.coerceIn(0.9f, 1.1f))

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = Typography,
            content = content
        )
    }
}
