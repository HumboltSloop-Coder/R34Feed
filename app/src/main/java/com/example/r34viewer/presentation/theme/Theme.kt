package com.example.r34viewer.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.r34viewer.domain.model.AppColors

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
    customColors: AppColors = AppColors(),
    content: @Composable () -> Unit
) {
    val base = if (darkTheme) DarkColorScheme else LightColorScheme
    val primary = customColors.primary.toColor() ?: base.primary
    val secondary = customColors.secondary.toColor() ?: base.secondary
    val background = customColors.background.toColor()
    val effectiveBackground = background ?: base.background

    val surface = background?.blendWith(base.surface, if (darkTheme) 0.35f else 0.08f) ?: base.surface
    val surfaceVariant = background?.blendWith(base.surfaceVariant, 0.22f) ?: base.surfaceVariant
    val primaryContainer = primary.blendWith(surface, 0.82f)
    val secondaryContainer = secondary.blendWith(surface, 0.82f)

    val colorScheme = base.copy(
        primary = primary,
        onPrimary = primary.onColor(),
        secondary = secondary,
        onSecondary = secondary.onColor(),
        tertiary = secondary,
        onTertiary = secondary.onColor(),
        background = effectiveBackground,
        onBackground = effectiveBackground.onColor(),
        surface = surface,
        onSurface = surface.onColor(),
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = surfaceVariant.onColor(),
        primaryContainer = primaryContainer,
        onPrimaryContainer = primaryContainer.onColor(),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = secondaryContainer.onColor()
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

private fun String?.toColor(): Color? {
    val raw = this?.trim()?.removePrefix("#") ?: return null
    if (raw.length != 6) return null
    val value = raw.toLongOrNull(16) ?: return null
    return Color((0xFF000000L or value).toInt())
}

private fun Color.blendWith(other: Color, fraction: Float): Color = copy(
    red = red + (other.red - red) * fraction,
    green = green + (other.green - green) * fraction,
    blue = blue + (other.blue - blue) * fraction
)

private fun Color.onColor(): Color {
    val luminance = 0.2126f * red + 0.7152f * green + 0.0722f * blue
    return if (luminance > 0.55f) Color.Black else Color.White
}
