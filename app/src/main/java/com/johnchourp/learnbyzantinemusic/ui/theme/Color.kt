package com.johnchourp.learnbyzantinemusic.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * The colour tokens the screens draw with (ClickUp `869f4tpju`).
 *
 * These used to be top-level constants. They are now **read from [LocalLbmPalette]**, which is what
 * makes dark mode possible without touching the 45 files that use them: every existing `LbmBrown`
 * reference already sits inside a `@Composable`, so it keeps compiling and starts following the
 * theme.
 *
 * `@ReadOnlyComposable` keeps them as cheap as the constants were — no recomposition scope is
 * created for reading one.
 *
 * The literal values live in [LbmPalette], one set per theme, so a colour is still defined exactly
 * once per theme and never spelled at a call site.
 */

val LbmBrown: Color @Composable @ReadOnlyComposable get() = LocalLbmPalette.current.brown
val LbmBrownSoft: Color @Composable @ReadOnlyComposable get() = LocalLbmPalette.current.brownSoft
val LbmPageBg: Color @Composable @ReadOnlyComposable get() = LocalLbmPalette.current.pageBg
val LbmSurface: Color @Composable @ReadOnlyComposable get() = LocalLbmPalette.current.surface
val LbmSurfaceVariant: Color @Composable @ReadOnlyComposable get() = LocalLbmPalette.current.surfaceVariant
val LbmOutline: Color @Composable @ReadOnlyComposable get() = LocalLbmPalette.current.outline
val LbmTextPrimary: Color @Composable @ReadOnlyComposable get() = LocalLbmPalette.current.textPrimary
val LbmTextSecondary: Color @Composable @ReadOnlyComposable get() = LocalLbmPalette.current.textSecondary
val LbmPrimaryContainer: Color @Composable @ReadOnlyComposable get() = LocalLbmPalette.current.primaryContainer
val LbmHeroStart: Color @Composable @ReadOnlyComposable get() = LocalLbmPalette.current.heroStart
val LbmHeroEnd: Color @Composable @ReadOnlyComposable get() = LocalLbmPalette.current.heroEnd

/** Crimson measure-bar accent, echoing the red χρόνος boundary bars in the neume diagrams. */
val LbmMeasureBar: Color @Composable @ReadOnlyComposable get() = LocalLbmPalette.current.measureBar

// Tile accent families (container background / icon-and-content tint).
val AccentGoldContainer: Color @Composable @ReadOnlyComposable get() = LocalLbmPalette.current.accentGoldContainer
val AccentGoldContent: Color @Composable @ReadOnlyComposable get() = LocalLbmPalette.current.accentGoldContent
val AccentBlueContainer: Color @Composable @ReadOnlyComposable get() = LocalLbmPalette.current.accentBlueContainer
val AccentBlueContent: Color @Composable @ReadOnlyComposable get() = LocalLbmPalette.current.accentBlueContent
val AccentPurpleContainer: Color @Composable @ReadOnlyComposable get() = LocalLbmPalette.current.accentPurpleContainer
val AccentPurpleContent: Color @Composable @ReadOnlyComposable get() = LocalLbmPalette.current.accentPurpleContent
val AccentOrangeContainer: Color @Composable @ReadOnlyComposable get() = LocalLbmPalette.current.accentOrangeContainer
val AccentOrangeContent: Color @Composable @ReadOnlyComposable get() = LocalLbmPalette.current.accentOrangeContent
val AccentGreenContainer: Color @Composable @ReadOnlyComposable get() = LocalLbmPalette.current.accentGreenContainer
val AccentGreenContent: Color @Composable @ReadOnlyComposable get() = LocalLbmPalette.current.accentGreenContent
val AccentBrownContainer: Color @Composable @ReadOnlyComposable get() = LocalLbmPalette.current.accentBrownContainer
val AccentBrownContent: Color @Composable @ReadOnlyComposable get() = LocalLbmPalette.current.accentBrownContent

/** Crimson «watch out» accent, for do/don't surfaces. */
val AccentCrimsonContainer: Color @Composable @ReadOnlyComposable get() = LocalLbmPalette.current.accentCrimsonContainer
val AccentCrimsonContent: Color @Composable @ReadOnlyComposable get() = LocalLbmPalette.current.accentCrimsonContent
