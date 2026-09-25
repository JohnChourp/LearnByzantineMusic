package com.johnchourp.learnbyzantinemusic.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * ClickUp `869f4tpju`: *«Ο χρωματικός κώδικας γένους (μαύρο/μπλε/μωβ/πορτοκαλί) παραμένει διακριτός
 * σε dark + περνά contrast check»*.
 *
 * "Looks fine to me" is not a contrast check, so this computes the real thing: WCAG 2.1 relative
 * luminance and contrast ratio, and asserts every palette clears the thresholds. The γένος colours
 * are **information** — they are how the four genera are told apart — so they are held to the
 * text threshold rather than the looser graphical one.
 *
 * Thresholds used:
 * - **4.5:1** for anything carrying meaning as text (WCAG AA normal text);
 * - two accents count as distinguishable at **20° of hue** *or* **1.5:1 of lightness**, because
 *   gold and orange are adjacent hues separated by brightness while purple and blue are the
 *   reverse — requiring either one alone would reject a pair that reads perfectly well.
 */
class GenusContrastTest {

    private val palettes = listOf(
        "light" to LbmPalette.light,
        "dark" to LbmPalette.dark,
        "highContrast" to LbmPalette.highContrast,
    )

    // The formula lives in Wcag so every contrast test shares it; the sanity check below pins it.
    private fun luminance(color: Color): Double = Wcag.luminance(color)

    private fun contrast(a: Color, b: Color): Double = Wcag.contrast(a, b)

    @Test
    fun theContrastFormulaIsItselfCorrect() {
        // Without this, a broken formula could make every assertion below pass.
        assertEquals(21.0, contrast(Color.Black, Color.White), 0.01)
        assertEquals(1.0, contrast(Color.White, Color.White), 0.001)
        assertEquals(contrast(Color.Black, Color.White), contrast(Color.White, Color.Black), 1e-9)
    }

    private fun genusPairs(p: LbmPalette) = listOf(
        "gold" to (p.accentGoldContent to p.accentGoldContainer),
        "purple" to (p.accentPurpleContent to p.accentPurpleContainer),
        "blue" to (p.accentBlueContent to p.accentBlueContainer),
        "orange" to (p.accentOrangeContent to p.accentOrangeContainer),
    )

    @Test
    fun everyGenusAccentIsReadableOnItsOwnContainer() {
        palettes.forEach { (name, palette) ->
            genusPairs(palette).forEach { (genus, pair) ->
                val ratio = contrast(pair.first, pair.second)
                assertTrue(
                    "$name/$genus: content on container is ${"%.2f".format(ratio)}:1, needs 4.5:1",
                    ratio >= 4.5,
                )
            }
        }
    }

    @Test
    fun everyGenusAccentIsReadableOnThePageAndOnCards() {
        palettes.forEach { (name, palette) ->
            genusPairs(palette).forEach { (genus, pair) ->
                listOf("page" to palette.pageBg, "surface" to palette.surface).forEach { (where, bg) ->
                    val ratio = contrast(pair.first, bg)
                    assertTrue(
                        "$name/$genus on $where is ${"%.2f".format(ratio)}:1, needs 4.5:1",
                        ratio >= 4.5,
                    )
                }
            }
        }
    }

    /** Hue angle in degrees, 0–360. */
    private fun hue(color: Color): Double {
        val r = color.red.toDouble(); val g = color.green.toDouble(); val b = color.blue.toDouble()
        val max = maxOf(r, g, b); val min = minOf(r, g, b)
        val delta = max - min
        if (delta < 1e-6) return 0.0
        val h = when (max) {
            r -> 60 * (((g - b) / delta) % 6)
            g -> 60 * (((b - r) / delta) + 2)
            else -> 60 * (((r - g) / delta) + 4)
        }
        return (h + 360) % 360
    }

    private fun hueSeparation(a: Color, b: Color): Double {
        val d = abs(hue(a) - hue(b))
        return minOf(d, 360 - d)
    }

    @Test
    fun theFourGenusAccentsStayDistinguishableFromEachOther() {
        // Measured by HUE, not by contrast ratio.
        //
        // Contrast ratio is a *luminance* comparison, and two colours can be equally bright while
        // being obviously different — gold against blue measures 1.21:1 and nobody would confuse
        // them. Using that number here would have demanded the four accents differ in brightness,
        // which is not what "distinguishable" means for a colour code.
        palettes.forEach { (name, palette) ->
            val accents = genusPairs(palette)
            accents.forEachIndexed { i, (genusA, pairA) ->
                accents.drop(i + 1).forEach { (genusB, pairB) ->
                    assertTrue(
                        "$name: $genusA and $genusB are literally the same colour",
                        pairA.first != pairB.first,
                    )
                    // Hue OR lightness. Gold and orange are genuinely adjacent hues — the design
                    // has always leaned on lightness to separate those two — so demanding hue alone
                    // would reject a pair that reads perfectly well. Demanding lightness alone would
                    // reject purple against blue. Either is enough; neither is not.
                    val hueApart = hueSeparation(pairA.first, pairB.first)
                    val lightnessApart = contrast(pairA.first, pairB.first)
                    assertTrue(
                        "$name: $genusA and $genusB are only ${"%.0f".format(hueApart)}° apart in hue " +
                            "and ${"%.2f".format(lightnessApart)}:1 apart in lightness — the colour " +
                            "code collapses",
                        hueApart >= 20.0 || lightnessApart >= 1.5,
                    )
                }
            }
        }
    }

    @Test
    fun bodyTextIsReadableEverywhereInEveryPalette() {
        palettes.forEach { (name, palette) ->
            listOf(
                "primary on page" to (palette.textPrimary to palette.pageBg),
                "primary on surface" to (palette.textPrimary to palette.surface),
                "secondary on page" to (palette.textSecondary to palette.pageBg),
                "secondary on surface" to (palette.textSecondary to palette.surface),
                "brand on page" to (palette.brown to palette.pageBg),
                "brand on surface" to (palette.brown to palette.surface),
                "brand on primaryContainer" to (palette.brown to palette.primaryContainer),
            ).forEach { (what, pair) ->
                val ratio = contrast(pair.first, pair.second)
                assertTrue(
                    "$name/$what is ${"%.2f".format(ratio)}:1, needs 4.5:1",
                    ratio >= 4.5,
                )
            }
        }
    }

    @Test
    fun highContrastIsActuallyHigherContrastThanDark() {
        // Otherwise the option is a lie. Compared on the measure that matters: body text.
        val darkRatio = contrast(LbmPalette.dark.textPrimary, LbmPalette.dark.pageBg)
        val hcRatio = contrast(LbmPalette.highContrast.textPrimary, LbmPalette.highContrast.pageBg)
        assertTrue(
            "high contrast (${"%.2f".format(hcRatio)}) must beat dark (${"%.2f".format(darkRatio)})",
            hcRatio > darkRatio,
        )
        assertTrue("high contrast body text should reach 15:1", hcRatio >= 15.0)
    }

    @Test
    fun theLightPaletteIsByteIdenticalToWhatShipped() {
        // Dark mode must not move the default appearance. These are the pre-existing values.
        val light = LbmPalette.light
        assertEquals(Color(0xFF7A4E24), light.brown)
        assertEquals(Color(0xFFF6F2EA), light.pageBg)
        assertEquals(Color(0xFFFFFCF7), light.surface)
        assertEquals(Color(0xFF2F251A), light.textPrimary)
        assertEquals(Color(0xFF655644), light.textSecondary)
        assertEquals(Color(0xFF0D47A1), light.accentBlueContent)
        assertEquals(Color(0xFF6A1B9A), light.accentPurpleContent)
        // Deliberately changed by this ticket — see the note in LbmPalette.light.
        assertEquals(Color(0xFFD0301C), light.accentOrangeContent)
        assertEquals(false, light.isDark)
    }

    @Test
    fun theDarkPalettesReallyAreDark() {
        listOf(LbmPalette.dark, LbmPalette.highContrast).forEach { palette ->
            assertTrue(palette.isDark)
            assertTrue(
                "a dark palette's page must be darker than its text",
                luminance(palette.pageBg) < luminance(palette.textPrimary),
            )
            assertTrue("page luminance should be very low", luminance(palette.pageBg) < 0.05)
        }
        assertTrue(
            "the light palette's page must be lighter than its text",
            luminance(LbmPalette.light.pageBg) > luminance(LbmPalette.light.textPrimary),
        )
    }

    @Test
    fun noPaletteIsACopyOfAnotherAndNoTokenIsTransparent() {
        // Guards two ways a palette goes wrong quietly: a copy-pasted theme that never actually
        // changes, and a token left at a default that happens to look right in one theme only.
        palettes.forEach { (name, palette) ->
            listOf(
                palette.brown, palette.pageBg, palette.surface, palette.textPrimary,
                palette.textSecondary, palette.outline, palette.measureBar,
                palette.accentGoldContent, palette.accentBlueContent,
                palette.accentPurpleContent, palette.accentOrangeContent,
            ).forEach { token ->
                assertTrue("$name has a non-opaque token", abs(token.alpha - 1f) < 0.001f)
            }
        }
        val backgrounds = palettes.map { it.second.pageBg }
        assertEquals("each palette needs its own background", backgrounds.size, backgrounds.distinct().size)
        val texts = palettes.map { it.second.textPrimary }
        assertEquals("each palette needs its own text colour", texts.size, texts.distinct().size)
    }
}
