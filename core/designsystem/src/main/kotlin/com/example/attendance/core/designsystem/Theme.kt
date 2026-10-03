package com.example.attendance.core.designsystem
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF355F50), onPrimary = Color.White,
    primaryContainer = Color(0xFFE0EEE3), onPrimaryContainer = Color(0xFF13382A),
    secondaryContainer = Color(0xFFE9F1E9), onSecondaryContainer = Color(0xFF253E30),
    background = Color(0xFFF7F9F5), onBackground = Color(0xFF1B3026),
    surface = Color(0xFFF7F9F5), onSurface = Color(0xFF1B3026),
    surfaceContainer = Color.White, surfaceContainerLow = Color(0xFFF0F4EE),
    surfaceContainerHigh = Color(0xFFE9F1E9), surfaceContainerHighest = Color(0xFFE0E9DF), outline = Color(0xFF758278),
    surfaceVariant = Color(0xFFE5ECE2), onSurfaceVariant = Color(0xFF53655A),
    outlineVariant = Color(0xFFDCE4DB), error = Color(0xFFAE3934))
private val DarkColors = darkColorScheme(
    primary = Color(0xFFA1D0B9), onPrimary = Color(0xFF073829),
    primaryContainer = Color(0xFF2B3A30), onPrimaryContainer = Color(0xFFD0EBD9),
    secondaryContainer = Color(0xFF2B3A30), onSecondaryContainer = Color(0xFFE3ECE5),
    background = Color(0xFF141B17), onBackground = Color(0xFFE3ECE5),
    surface = Color(0xFF141B17), onSurface = Color(0xFFE3ECE5),
    surfaceContainer = Color(0xFF202A23), surfaceContainerLow = Color(0xFF1B251E),
    surfaceContainerHigh = Color(0xFF2B3A30), surfaceContainerHighest = Color(0xFF354439), outline = Color(0xFF8D9C91),
    onSurfaceVariant = Color(0xFFADBDAF))
@Composable fun AttendanceTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, typography = Typography(), content = content)
}
