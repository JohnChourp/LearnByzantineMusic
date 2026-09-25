package com.johnchourp.learnbyzantinemusic.modes

import androidx.compose.ui.graphics.Color
import com.johnchourp.learnbyzantinemusic.docs.KotlinSource
import com.johnchourp.learnbyzantinemusic.ui.theme.AndroidColorResources
import com.johnchourp.learnbyzantinemusic.ui.theme.Wcag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs

/**
 * ClickUp `869f5x286`: the pages / search menu of «8 Ήχοι» — which the home screen's search opens
 * too — was a white card in dark mode, with the dark theme's near-white text on it (1.19:1).
 *
 * The menu is a View dialog (`EightModesNavigation.showMenu`), so its colours are XML resources and
 * no unit test can inflate it. This reads those resources the way Android resolves them, with and
 * without the night flag, and measures every text colour the menu uses against every surface it
 * draws text on.
 *
 * A contrast check over resources proves nothing if the code draws with something else — the fill
 * that broke was a literal `WHITE` in Kotlin, not a resource. So a source guard also holds the menu
 * to colours this test measures: no literal colour, and no resource it does not know about.
 */
class PagesMenuContrastTest {

    private class Surface(val label: String, val colorName: String? = null, val drawableName: String? = null) {
        fun resolve(night: Boolean): Color = when {
            colorName != null -> AndroidColorResources.color(colorName, night)
            drawableName != null -> AndroidColorResources.drawableSolid(drawableName, night)
            else -> error("a surface needs a colour or a drawable")
        }
    }

    /** Every colour the menu draws text in: titles and rows, the hint and «no results», the selected row. */
    private val textColors = listOf(
        "first_mode_theory_text_primary",
        "first_mode_theory_text_secondary",
        "first_mode_theory_accent",
    )

    /** Every surface the menu draws text on. */
    private val surfaces = listOf(
        Surface("menu card", colorName = "first_mode_theory_card_bg"),
        Surface("search field", drawableName = "eight_modes_spinner_bg"),
        Surface("selected row", colorName = "first_mode_theory_callout_bg"),
    )

    /** Strokes: drawn, but never under text, so they are not held to the text threshold. */
    private val strokeColors = setOf("first_mode_theory_card_border", "first_mode_theory_callout_border")

    private val menuSource: File by lazy {
        File(KotlinSource.mainRoot, "com/johnchourp/learnbyzantinemusic/modes/EightModesNavigation.kt")
    }

    @Test
    fun everyMenuTextIsReadableOnEveryMenuSurface() {
        var checked = 0
        val failures = listOf(false, true).flatMap { night ->
            val mode = if (night) "night" else "light"
            surfaces.flatMap { surface ->
                val background = surface.resolve(night)
                assertTrue("$mode/${surface.label} is not opaque", abs(background.alpha - 1f) < 0.001f)
                textColors.mapNotNull { name ->
                    checked++
                    val ratio = Wcag.contrast(AndroidColorResources.color(name, night), background)
                    if (ratio >= Wcag.TEXT_AA) null
                    else "$mode: $name on the ${surface.label} is ${"%.2f".format(ratio)}:1, needs ${Wcag.TEXT_AA}:1"
                }
            }
        }
        // Guards the slice: 2 night settings × 3 surfaces × 3 text colours.
        assertEquals(18, checked)
        assertEquals(emptyList<String>(), failures)
    }

    @Test
    fun theCheckRejectsTheMenuThatShipped() {
        // Negative control: the shipped dark-mode menu — white card, night text — must fail the
        // threshold used above, or that assertion could never go red.
        val nightText = AndroidColorResources.color("first_mode_theory_text_primary", night = true)
        val ratio = Wcag.contrast(nightText, AndroidColorResources.parseHex("#FFFFFFFF"))
        assertTrue("measured ${"%.2f".format(ratio)}:1", ratio < Wcag.TEXT_AA)
    }

    /** An opaque colour spelled in code rather than taken from a resource. TRANSPARENT is fine. */
    private val literalColor = Regex(
        """\bColor\.(WHITE|BLACK|GRAY|DKGRAY|LTGRAY|RED|GREEN|BLUE|YELLOW|CYAN|MAGENTA)\b|""" +
            """\bColor\.(rgb|argb|parseColor|valueOf)\s*\(|\b0x[0-9A-Fa-f]{6,8}\b"""
    )

    @Test
    fun theMenuDrawsOnlyWithColoursThisTestMeasures() {
        val code = KotlinSource.withoutComments(menuSource.readText())

        assertEquals(
            "the menu must take every opaque colour from a resource",
            emptyList<String>(),
            literalColor.findAll(code).map { it.value }.toList(),
        )

        val known = textColors + surfaces.mapNotNull { it.colorName } + strokeColors
        val usedColors = Regex("""\bR\.color\.(\w+)""").findAll(code).map { it.groupValues[1] }.toSet()
        assertTrue("expected the menu's colour resources, found $usedColors", usedColors.size >= 5)
        assertEquals("add these to the text colours, surfaces or strokes above", emptyList<String>(), (usedColors - known.toSet()).sorted())

        val usedDrawables = Regex("""\bR\.drawable\.(\w+)""").findAll(code).map { it.groupValues[1] }.toSet()
        assertEquals(
            "add these to the surfaces above",
            emptyList<String>(),
            (usedDrawables - surfaces.mapNotNull { it.drawableName }.toSet()).sorted(),
        )
    }

    @Test
    fun theSourceGuardCatchesALiteral() {
        // Negative control for the guard itself: it must see the literal that shipped, and must not
        // object to TRANSPARENT (an unselected row has no fill of its own) or to a comment about it.
        assertTrue(literalColor.containsMatchIn("fillColor = Color.WHITE,"))
        assertTrue(literalColor.containsMatchIn("setColor(Color.parseColor(\"#FFFFFF\"))"))
        assertTrue(literalColor.containsMatchIn("fillColor = 0xFFFFFFFF.toInt(),"))
        assertFalse(literalColor.containsMatchIn("fillColor = Color.TRANSPARENT,"))
        assertFalse(literalColor.containsMatchIn(KotlinSource.withoutComments("// once Color.WHITE\nval x = 1")))
    }
}
