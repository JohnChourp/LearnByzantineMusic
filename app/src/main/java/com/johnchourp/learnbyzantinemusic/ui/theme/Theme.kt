package com.johnchourp.learnbyzantinemusic.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LbmShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
)

/**
 * App theme, in three variants (ClickUp `869f4tpju`).
 *
 * The chosen [palette] is provided through [LocalLbmPalette], which is what every `LbmBrown`-style
 * token reads. Material3's own scheme is derived from the same palette, so a Material component and
 * a hand-drawn one cannot end up on different colours.
 *
 * `onPrimary`/`onSecondary` follow the palette's own ground rather than being hard-coded white: on
 * the dark palettes the primary is a light gold, and white text on it would be unreadable.
 */
@Composable
fun LbmTheme(
    palette: LbmPalette = LbmPalette.light,
    content: @Composable () -> Unit,
) {
    val onAccent = if (palette.isDark) palette.pageBg else Color.White
    val scheme = if (palette.isDark) {
        darkColorScheme(
            primary = palette.brown,
            onPrimary = onAccent,
            primaryContainer = palette.primaryContainer,
            onPrimaryContainer = palette.textPrimary,
            secondary = palette.brownSoft,
            onSecondary = onAccent,
            background = palette.pageBg,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceVariant,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.outline,
        )
    } else {
        lightColorScheme(
            primary = palette.brown,
            onPrimary = onAccent,
            primaryContainer = palette.primaryContainer,
            onPrimaryContainer = palette.textPrimary,
            secondary = palette.brownSoft,
            onSecondary = onAccent,
            background = palette.pageBg,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceVariant,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.outline,
        )
    }

    CompositionLocalProvider(LocalLbmPalette provides palette) {
        MaterialTheme(
            colorScheme = scheme,
            typography = LbmTypography,
            shapes = LbmShapes,
            content = content,
        )
    }
}
