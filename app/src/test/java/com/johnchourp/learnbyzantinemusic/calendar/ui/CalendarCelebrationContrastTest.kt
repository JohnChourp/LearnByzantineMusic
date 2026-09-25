package com.johnchourp.learnbyzantinemusic.calendar.ui

import androidx.compose.ui.graphics.Color
import com.johnchourp.learnbyzantinemusic.calendar.CalendarCelebrationType
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPalette
import com.johnchourp.learnbyzantinemusic.ui.theme.Wcag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * ClickUp `869f5x286`: *«Υπολογισμένη αντίθεση ≥ 4,5:1 για το κείμενο των καρτών εορτών … σε
 * φωτεινό / σκούρο / υψηλής αντίθεσης»*.
 *
 * In dark mode the Ημερολόγιο's celebration cards kept a light background (a colour resource with
 * no night value) while their text followed the theme: 1.08–1.14:1 for the title, 1.04–1.10:1 in
 * high contrast. The light theme's badges failed too — the half-holiday badge measured 2.46:1.
 *
 * Measures every card type in every palette through [celebrationColors], the function the screen
 * itself draws with, so a ratio here is a ratio on the device:
 * - the title and the description on the card;
 * - the type badge's text on the badge's own fill (the accent tinted over the card).
 */
class CalendarCelebrationContrastTest {

    private val palettes = listOf(
        "light" to LbmPalette.light,
        "dark" to LbmPalette.dark,
        "highContrast" to LbmPalette.highContrast,
    )

    private fun textPairs(colors: CelebrationColors) = listOf(
        "title on card" to (colors.title to colors.background),
        "description on card" to (colors.description to colors.background),
        "badge text on badge" to (colors.accent to colors.badgeFill),
    )

    @Test
    fun everyCardIsReadableInEveryPalette() {
        var checked = 0
        val failures = palettes.flatMap { (name, palette) ->
            CalendarCelebrationType.entries.flatMap { type ->
                textPairs(celebrationColors(type, palette)).mapNotNull { (what, pair) ->
                    checked++
                    val ratio = Wcag.contrast(pair.first, pair.second)
                    if (ratio >= Wcag.TEXT_AA) null
                    else "$name/$type: $what is ${"%.2f".format(ratio)}:1, needs ${Wcag.TEXT_AA}:1"
                }
            }
        }
        // Guards the slice: 3 palettes × 4 types × 3 pairs. Fewer means something was skipped.
        assertEquals(3 * CalendarCelebrationType.entries.size * 3, checked)
        assertTrue("expected all four celebration types", CalendarCelebrationType.entries.size >= 4)
        assertEquals(emptyList<String>(), failures)
    }

    @Test
    fun theCheckRejectsTheCardThatShipped() {
        // Negative control: the shipped dark-mode card — light background, dark palette's text —
        // must fail the very threshold used above, or that assertion could never go red.
        val shippedCard = Color(0xFFFFF1F1)
        val ratio = Wcag.contrast(LbmPalette.dark.textPrimary, shippedCard)
        assertTrue("measured ${"%.2f".format(ratio)}:1", ratio < Wcag.TEXT_AA)
    }

    @Test
    fun theDarkPalettesPaintDarkCards() {
        // The symptom as reported: «ανοιχτό φόντο με ανοιχτό κείμενο». Contrast alone would also
        // pass a light card with dark text, which in a dark palette is a glaring light island.
        listOf("dark" to LbmPalette.dark, "highContrast" to LbmPalette.highContrast).forEach { (name, palette) ->
            CalendarCelebrationType.entries.forEach { type ->
                val card = celebrationColors(type, palette).background
                assertTrue(
                    "$name/$type card luminance ${"%.3f".format(Wcag.luminance(card))} is not dark",
                    Wcag.luminance(card) < 0.05,
                )
            }
        }
    }

    @Test
    fun everyCelebrationColourIsOpaque() {
        // A translucent colour's contrast depends on whatever is behind it, which is not measured.
        palettes.forEach { (name, palette) ->
            CalendarCelebrationType.entries.forEach { type ->
                val colors = celebrationColors(type, palette)
                listOf(colors.background, colors.border, colors.accent, colors.badgeFill).forEach { color ->
                    assertTrue("$name/$type has a non-opaque colour", abs(color.alpha - 1f) < 0.001f)
                }
            }
        }
    }

    @Test
    fun theLightCardsKeepTheColoursTheyShippedWith() {
        // Only the light badges were meant to change. These are the deleted
        // weekly_calendar_celebration_* resources, so the light cards look exactly as before.
        val light = LbmPalette.light
        assertEquals(Color(0xFFFFF1F1), light.celebrationPublicBg)
        assertEquals(Color(0xFFF3BBBB), light.celebrationPublicBorder)
        assertEquals(Color(0xFFFFF7ED), light.celebrationHalfBg)
        assertEquals(Color(0xFFF4D2A6), light.celebrationHalfBorder)
        assertEquals(Color(0xFFF1F7FF), light.celebrationReligiousBg)
        assertEquals(Color(0xFFB9D1F3), light.celebrationReligiousBorder)
        assertEquals(Color(0xFFF9FAFC), light.celebrationNormalBg)
        assertEquals(Color(0xFFD8DEE8), light.celebrationNormalBorder)
    }
}
