package com.johnchourp.learnbyzantinemusic.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LbmLightColors = lightColorScheme(
    primary = LbmBrown,
    onPrimary = Color.White,
    primaryContainer = LbmPrimaryContainer,
    onPrimaryContainer = LbmTextPrimary,
    secondary = LbmBrownSoft,
    onSecondary = Color.White,
    background = LbmPageBg,
    onBackground = LbmTextPrimary,
    surface = LbmSurface,
    onSurface = LbmTextPrimary,
    surfaceVariant = LbmSurfaceVariant,
    onSurfaceVariant = LbmTextSecondary,
    outline = LbmOutline,
)

private val LbmShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
)

/**
 * App theme for the redesigned home screen. Light-only Material3 theme tuned to the
 * Byzantine warm palette. Sub-screens keep their own XML themes; this only wraps Compose content.
 */
@Composable
fun LbmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LbmLightColors,
        typography = LbmTypography,
        shapes = LbmShapes,
        content = content,
    )
}
