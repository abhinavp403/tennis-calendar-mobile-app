package dev.abhinav.tenniscalendar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dark palette.
internal val Background = Color(0xFF0B1020)
internal val Surface = Color(0xFF111827)
internal val SurfaceVariant = Color(0xFF1F2937)
internal val OnSurface = Color(0xFFE5E7EB)
internal val OnSurfaceMuted = Color(0xFF9CA3AF)
internal val Outline = Color(0xFF374151)
internal val Violet = Color(0xFFA78BFA)

private val TennisDarkScheme = darkColorScheme(
    primary = Violet,
    onPrimary = Color(0xFF1A1030),
    secondary = Color(0xFF60A5FA),
    onSecondary = Color(0xFF06122A),
    tertiary = Color(0xFFBE398D),
    background = Background,
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceMuted,
    outline = Outline,
    outlineVariant = Color(0xFF2A3444),
)

private val TennisLightScheme = lightColorScheme(
    primary = Color(0xFF7C3AED),
    onPrimary = Color.White,
    secondary = Color(0xFF2563EB),
    onSecondary = Color.White,
    tertiary = Color(0xFFBE398D),
    background = Color(0xFFF6F7F9),
    onBackground = Color(0xFF111827),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFEEF1F5),
    onSurfaceVariant = Color(0xFF6B7280),
    outline = Color(0xFFD1D5DB),
    outlineVariant = Color(0xFFE5E7EB),
)

@Composable
fun TennisCalendarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) TennisDarkScheme else TennisLightScheme,
        typography = Typography,
        content = content,
    )
}
