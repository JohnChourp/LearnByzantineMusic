package com.johnchourp.learnbyzantinemusic.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The View-side copy of the theme stays equal to the palette (ClickUp `869f5x286`).
 *
 * `res/values-night/colors.xml` has said since dark mode shipped (`869f4tpju`) that
 * "ThemeXmlParityTest asserts the two stay in step" — and no such test existed. This is it.
 *
 * Why the copy exists at all: two theory screens (`ModeTheoryActivity`, `TheoryTopicActivity`) and
 * the pages menu are Views, and XML cannot read a Compose CompositionLocal, so their colours are a
 * second copy of [LbmPalette] in `first_mode_theory_*` resources. A second copy drifts silently: a
 * theory page opened from a Compose screen would change character halfway, and nothing would fail.
 *
 * - `values` must equal [LbmPalette.light], `values-night` must equal [LbmPalette.dark]. High
 *   contrast has no resource qualifier, so a View screen falls back to the night values there.
 * - Every colour of the family must have a night twin — a light-only one is exactly the bug the
 *   calendar cards had.
 * - Every colour of the family must be mirrored or be a *named* exception with its reason, so a new
 *   colour cannot join the family without someone deciding what it mirrors.
 */
class ThemeXmlParityTest {

    private val family = "first_mode_theory_"

    /** Resource → the palette field it mirrors. */
    private val mirrors: Map<String, (LbmPalette) -> Color> = mapOf(
        "first_mode_theory_page_bg" to { it.pageBg },
        "first_mode_theory_card_bg" to { it.surface },
        "first_mode_theory_card_border" to { it.outline },
        "first_mode_theory_callout_bg" to { it.primaryContainer },
        "first_mode_theory_hero_start" to { it.heroStart },
        "first_mode_theory_hero_end" to { it.heroEnd },
        "first_mode_theory_accent" to { it.brown },
        "first_mode_theory_accent_soft" to { it.brownSoft },
        "first_mode_theory_text_primary" to { it.textPrimary },
        "first_mode_theory_text_secondary" to { it.textSecondary },
    )

    /** Colours of the family with no palette field, and why. */
    private val exceptions: Map<String, String> = mapOf(
        "first_mode_theory_callout_border" to
            "only the View screens draw a callout border — Compose callouts are a borderless " +
            "LbmPrimaryContainer — so there is no palette field for it to mirror",
    )

    private fun familyNames(valuesDir: String): Set<String> =
        AndroidColorResources.rawColors(valuesDir).keys.filter { it.startsWith(family) }.toSet()

    @Test
    fun theColoursAreActuallyFound() {
        // Guards the slice: a wrong path or a broken pattern would make every check below pass
        // over nothing.
        assertTrue("light family: ${familyNames("values")}", familyNames("values").size >= 10)
        assertTrue("night family: ${familyNames("values-night")}", familyNames("values-night").size >= 10)
    }

    @Test
    fun everyColourOfTheFamilyHasANightTwin() {
        assertEquals(
            "a colour without a night twin stays light in dark mode; one without a light value " +
                "does not resolve at all in light mode",
            familyNames("values").sorted(),
            familyNames("values-night").sorted(),
        )
    }

    @Test
    fun everyColourIsMirroredOrANamedException() {
        val unaccounted = (familyNames("values") + familyNames("values-night"))
            .filter { it !in mirrors && it !in exceptions }
            .sorted()
        assertEquals("map each to its palette field, or name it as an exception with the reason", emptyList<String>(), unaccounted)

        val stale = (mirrors.keys + exceptions.keys).filter { it !in familyNames("values") }.sorted()
        assertEquals("these no longer exist; drop them from the test", emptyList<String>(), stale)

        assertEquals("a colour is either mirrored or an exception, never both", emptySet<String>(), mirrors.keys intersect exceptions.keys)
    }

    @Test
    fun theLightValuesAreTheLightPalette() {
        assertMirrors(night = false, palette = LbmPalette.light, paletteName = "LbmPalette.light")
    }

    @Test
    fun theNightValuesAreTheDarkPalette() {
        assertMirrors(night = true, palette = LbmPalette.dark, paletteName = "LbmPalette.dark")
    }

    private fun assertMirrors(night: Boolean, palette: LbmPalette, paletteName: String) {
        val drifted = mirrors.mapNotNull { (name, field) ->
            val xml = AndroidColorResources.color(name, night)
            val expected = field(palette)
            if (xml == expected) null else "$name is ${hex(xml)}, $paletteName says ${hex(expected)}"
        }
        assertEquals("the View screens would not match the Compose ones", emptyList<String>(), drifted)
    }

    @Test
    fun theComparisonCanFail() {
        // Negative control: the two sides really are compared as colours — a light value read
        // under the night flag must NOT match the dark palette, or the checks above prove nothing.
        assertTrue(AndroidColorResources.color("first_mode_theory_page_bg", night = false) != LbmPalette.dark.pageBg)
        assertEquals(Color(0xFFF6F2EA), AndroidColorResources.parseHex("#FFF6F2EA"))
        assertEquals(Color(0xFF97B9E8), AndroidColorResources.parseHex("#97B9E8"))
    }

    private fun hex(color: Color): String = "#%08X".format(color.toArgb())
}
