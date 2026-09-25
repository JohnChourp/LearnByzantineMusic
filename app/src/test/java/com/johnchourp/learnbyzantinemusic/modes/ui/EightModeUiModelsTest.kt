package com.johnchourp.learnbyzantinemusic.modes.ui

import com.johnchourp.learnbyzantinemusic.modes.EightModeScaleDefinitions
import com.johnchourp.learnbyzantinemusic.music.Mode
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ClickUp `869f4tpxj`: binds the screen's table of ήχοι to the typed domain.
 *
 * The UI listed the eight modes in **display** order with a scale written next to each, while
 * `EightModeScaleDefinitions` held the canonical mode→scale table in **theory** order. Two
 * hand-maintained copies of the same fact, in different orders, with nothing checking them against
 * each other: getting one wrong would mis-tune a whole ήχος and nothing would complain.
 */
class EightModeUiModelsTest {

    @Test
    fun theScreenListsAllEightModesExactlyOnce() {
        assertEquals(8, EIGHT_MODES.size)
        val modes = EIGHT_MODES.map { it.mode }
        assertEquals("every ήχος appears once", Mode.entries.toSet(), modes.toSet())
        assertEquals("no duplicates", EIGHT_MODES.size, modes.distinct().size)
    }

    @Test
    fun everyRowUsesTheScaleTheCanonicalTableAssignsThatMode() {
        EIGHT_MODES.forEach { row ->
            val mode = row.mode
            assertEquals(
                "${mode.key}: the screen and the canonical table disagree about the scale",
                EightModeScaleDefinitions.SCALE_BY_MODE.getValue(mode),
                row.scale,
            )
        }
    }

    @Test
    fun theDisplayOrderIsDeliberateAndNotTheTheoryOrder() {
        // Pins the fact that these two orders differ on purpose — the selector groups by γένος.
        // If someone "aligns" them, this fails and they have to mean it.
        val displayOrder = EIGHT_MODES.map { it.mode }
        assertEquals(
            listOf(
                Mode.FIRST, Mode.FOURTH, Mode.PLAGAL_FIRST, Mode.PLAGAL_FOURTH,
                Mode.THIRD, Mode.VARYS, Mode.SECOND, Mode.PLAGAL_SECOND,
            ),
            displayOrder,
        )
        assertEquals(Mode.entries.toList(), EightModeScaleDefinitions.SCALE_BY_MODE.keys.toList())
    }

    @Test
    fun theStoredKeySpellingIsUnchanged() {
        // These strings are inside preference names (mode_base_shift_moria_<key>). A rename would
        // silently reset every user's «Μεταφορά βάσης» for that mode.
        assertEquals(
            listOf(
                "first", "second", "third", "fourth",
                "plagal_first", "plagal_second", "varys", "plagal_fourth",
            ),
            Mode.entries.map { it.key },
        )
        EIGHT_MODES.forEach { row -> assertEquals(row.theoryKey, row.mode.key) }
    }
}
