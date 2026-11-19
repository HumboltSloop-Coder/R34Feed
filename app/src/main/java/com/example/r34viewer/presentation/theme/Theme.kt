package com.example.r34viewer.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BrandGreen,
    onPrimary = Color.Black,
    secondary = BrandGreenSecondary,
    onSecondary = Color.Black,
    tertiary = BrandGreenTertiary,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFB8B8C0),
    error = Color(0xFFFF6B6B)
)

private val LightColorScheme = lightColorScheme(
    primary = BrandGreen,
    onPrimary = Color.Black,
    secondary = BrandGreenSecondary,
    onSecondary = Color.Black,
    tertiary = BrandGreenTertiary,
    onTertiary = Color.Black,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = Color(0xFFF1F1F4),
    onSurfaceVariant = Color(0xFF55555F),
    error = Color(0xFFB3261E)
)

@Composable
fun R34ViewerTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
