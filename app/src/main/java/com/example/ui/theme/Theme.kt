package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ManznimeDarkColorScheme = darkColorScheme(
    primary = ElectricCrimson,
    onPrimary = Color.White,
    primaryContainer = CrimsonContainer,
    onPrimaryContainer = OnCrimsonContainer,
    secondary = LuminousCyan,
    onSecondary = Color(0xFF001F26),
    secondaryContainer = CyanContainer,
    onSecondaryContainer = OnCyanContainer,
    tertiary = StarGold,
    onTertiary = Color(0xFF261900),
    background = ObsidianNight,
    onBackground = TextPrimary,
    surface = CharcoalSurface,
    onSurface = TextPrimary,
    surfaceVariant = ElevatedCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianNight,
    surfaceContainerLow = CharcoalSurface,
    surfaceContainer = ElevatedCard,
    surfaceContainerHigh = GlassOverlay,
    outline = HairlineBorder,
    outlineVariant = Color(0xFF1E2436),
    error = ErrorCoral,
    onError = Color.White
)

val ManznimeShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ManznimeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ManznimeDarkColorScheme,
        typography = ManznimeTypography,
        shapes = ManznimeShapes,
        content = content
    )
}
