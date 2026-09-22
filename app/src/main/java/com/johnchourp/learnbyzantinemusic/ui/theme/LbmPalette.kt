package com.johnchourp.learnbyzantinemusic.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Every colour the app draws with, as one swappable set (ClickUp `869f4tpju`).
 *
 * ## Why the tokens became a palette
 *
 * The colours used to be top-level `val`s — compile-time constants. That is fine for one theme and
 * impossible for two: a constant cannot know whether the screen is light or dark. Collecting them
 * into a value that a [LocalLbmPalette] provides means every existing `LbmBrown` reference keeps
 * reading exactly as before while becoming theme-aware, instead of 45 files each learning about
 * themes.
 *
 * ## The three palettes
 *
 * - [light] — unchanged from what shipped. Byte-for-byte the previous values, so the default
 *   appearance does not move.
 * - [dark] — for reading off a stand in a dimly lit ἀναλόγιο, which is where this app is actually
 *   used. Not an inversion: the warm Byzantine character is kept, backgrounds go deep brown-black
 *   rather than pure black, and the accents are lightened until they carry on a dark surface.
 * - [highContrast] — a darker-still background with maximally separated accents, for low vision.
 *
 * ## The constraint that shaped the dark accents
 *
 * The γένος colour code (gold, purple, blue, orange) is **information**, not decoration: it is how
 * the four genera are told apart at a glance. The light accents are mid-tones chosen against a cream
 * background and several of them fail against a dark one. So the dark set is not the light set
 * dimmed — each accent was lightened until it clears WCAG on its own surface, and
 * `GenusContrastTest` computes the ratios rather than trusting the eye.
 */
data class LbmPalette(
    val brown: Color,
    val brownSoft: Color,
    val pageBg: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val outline: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val primaryContainer: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val measureBar: Color,
    val accentGoldContainer: Color,
    val accentGoldContent: Color,
    val accentBlueContainer: Color,
    val accentBlueContent: Color,
    val accentPurpleContainer: Color,
    val accentPurpleContent: Color,
    val accentOrangeContainer: Color,
    val accentOrangeContent: Color,
    val accentGreenContainer: Color,
    val accentGreenContent: Color,
    val accentBrownContainer: Color,
    val accentBrownContent: Color,
    val accentCrimsonContainer: Color,
    val accentCrimsonContent: Color,
    val isDark: Boolean,
) {
    companion object {
        /** Exactly the values that shipped before dark mode existed. */
        val light = LbmPalette(
            brown = Color(0xFF7A4E24),
            brownSoft = Color(0xFFA16839),
            pageBg = Color(0xFFF6F2EA),
            surface = Color(0xFFFFFCF7),
            surfaceVariant = Color(0xFFF2E8D8),
            outline = Color(0xFFE1D2B8),
            textPrimary = Color(0xFF2F251A),
            textSecondary = Color(0xFF655644),
            primaryContainer = Color(0xFFF7E8D3),
            heroStart = Color(0xFFEADCC5),
            heroEnd = Color(0xFFF7EFE1),
            measureBar = Color(0xFFA0003C),
            accentGoldContainer = Color(0xFFF7E8D3),
            accentGoldContent = Color(0xFF7A4E24),
            accentBlueContainer = Color(0xFFE6F0FF),
            accentBlueContent = Color(0xFF0D47A1),
            accentPurpleContainer = Color(0xFFF0E6F7),
            accentPurpleContent = Color(0xFF6A1B9A),
            accentOrangeContainer = Color(0xFFFFF1E0),
            // Changed from the shipped 0xFFE65100, which measured only 3.41:1 on its own container
            // and 3.39:1 on the page — below WCAG AA, for a colour that carries meaning (it IS the
            // enharmonic genus). 0xFFD0301C clears 4.5:1 on both.
            //
            // Simply darkening the old orange was not enough: it fixed the contrast and destroyed
            // the separation from gold, because the two are adjacent hues that the light theme was
            // relying on LIGHTNESS to tell apart. This value is shifted toward red instead, giving
            // 22.6° of hue separation. See GenusContrastTest.
            accentOrangeContent = Color(0xFFD0301C),
            accentGreenContainer = Color(0xFFE6F4EA),
            accentGreenContent = Color(0xFF2E7D32),
            accentBrownContainer = Color(0xFFEFE3D2),
            accentBrownContent = Color(0xFF5D4037),
            accentCrimsonContainer = Color(0xFFF7E0E8),
            accentCrimsonContent = Color(0xFFA0003C),
            isDark = false,
        )

        /** Warm dark, for a dimly lit stand. Accents lightened until they carry on a dark surface. */
        val dark = LbmPalette(
            brown = Color(0xFFE8B981),
            brownSoft = Color(0xFFCFA070),
            pageBg = Color(0xFF17120D),
            surface = Color(0xFF211A13),
            surfaceVariant = Color(0xFF2B2219),
            outline = Color(0xFF4A3C2C),
            textPrimary = Color(0xFFF3EADC),
            textSecondary = Color(0xFFC3B5A1),
            primaryContainer = Color(0xFF3A2C1C),
            heroStart = Color(0xFF2A2017),
            heroEnd = Color(0xFF1C1610),
            measureBar = Color(0xFFFF7BA5),
            accentGoldContainer = Color(0xFF3A2C1C),
            accentGoldContent = Color(0xFFF0C48A),
            accentBlueContainer = Color(0xFF16243A),
            accentBlueContent = Color(0xFF9CC4FF),
            accentPurpleContainer = Color(0xFF2B1B38),
            accentPurpleContent = Color(0xFFD7A9F5),
            accentOrangeContainer = Color(0xFF3A2413),
            // Not the light orange lightened: at 0xFFFFB077 it sat 9° from gold with only 1.11:1
            // of lightness between them, which would have collapsed two genera into one colour on
            // a dark screen. This is more saturated and deeper, giving 1.70:1 against gold.
            accentOrangeContent = Color(0xFFFF7043),
            accentGreenContainer = Color(0xFF142A19),
            accentGreenContent = Color(0xFF8FD69B),
            accentBrownContainer = Color(0xFF2E241B),
            accentBrownContent = Color(0xFFD9BFA3),
            accentCrimsonContainer = Color(0xFF3A1522),
            accentCrimsonContent = Color(0xFFFF7BA5),
            isDark = true,
        )

        /** Maximum separation for low vision: near-black ground, accents pushed toward white. */
        val highContrast = LbmPalette(
            brown = Color(0xFFFFD79A),
            brownSoft = Color(0xFFFFC77A),
            pageBg = Color(0xFF000000),
            surface = Color(0xFF0B0B0B),
            surfaceVariant = Color(0xFF161616),
            outline = Color(0xFF8A7A66),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFE2D9CB),
            primaryContainer = Color(0xFF231A0E),
            heroStart = Color(0xFF141414),
            heroEnd = Color(0xFF000000),
            measureBar = Color(0xFFFF9EBF),
            accentGoldContainer = Color(0xFF231A0E),
            accentGoldContent = Color(0xFFFFD79A),
            accentBlueContainer = Color(0xFF0A1626),
            accentBlueContent = Color(0xFFBFD9FF),
            accentPurpleContainer = Color(0xFF1C1026),
            accentPurpleContent = Color(0xFFE7C6FF),
            accentOrangeContainer = Color(0xFF261603),
            // Same trap as the dark palette, worse: 0xFFFFC79A and the gold were both pale peach,
            // 9.5° apart. Shifted toward red for 25.8° of separation.
            accentOrangeContent = Color(0xFFFF9B86),
            accentGreenContainer = Color(0xFF071A0C),
            accentGreenContent = Color(0xFFB4EEBE),
            accentBrownContainer = Color(0xFF1E1710),
            accentBrownContent = Color(0xFFEDD9C2),
            accentCrimsonContainer = Color(0xFF260D16),
            accentCrimsonContent = Color(0xFFFF9EBF),
            isDark = true,
        )
    }
}

/**
 * The palette in force. Static because a theme change recomposes the whole tree anyway, and a
 * static local avoids re-reading it on every colour access.
 */
val LocalLbmPalette = staticCompositionLocalOf { LbmPalette.light }
