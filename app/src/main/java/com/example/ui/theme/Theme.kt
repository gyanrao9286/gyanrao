package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ExecutiveDarkColorScheme = darkColorScheme(
    primary = AuraAmber,
    onPrimary = Color(0xFF0A0F1D),
    primaryContainer = Color(0xFF3B2A0C),
    onPrimaryContainer = Color(0xFFFDE68A),
    secondary = AuraCyan,
    onSecondary = Color(0xFF062032),
    secondaryContainer = Color(0xFF0E3A5A),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = AuraEmerald,
    onTertiary = Color(0xFF042F24),
    tertiaryContainer = Color(0xFF094938),
    onTertiaryContainer = Color(0xFFD1FAE5),
    background = MidnightBg,
    onBackground = TextPrimaryDark,
    surface = MidnightSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = MidnightCard,
    onSurfaceVariant = TextSecondaryDark,
    outline = MidnightBorder,
    error = AuraRose,
    onError = Color.White
)

private val ExecutiveLightColorScheme = lightColorScheme(
    primary = Color(0xFFD97706),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF78350F),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = Color(0xFF059669),
    onTertiary = Color.White,
    background = DaylightBg,
    onBackground = TextPrimaryLight,
    surface = DaylightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = DaylightCard,
    onSurfaceVariant = TextSecondaryLight,
    error = AuraRose,
    onError = Color.White
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ExecutiveDarkColorScheme else ExecutiveLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
