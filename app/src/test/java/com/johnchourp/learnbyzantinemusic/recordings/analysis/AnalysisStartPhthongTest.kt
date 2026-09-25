package com.johnchourp.learnbyzantinemusic.recordings.analysis

import com.johnchourp.learnbyzantinemusic.modes.ModeResources
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import com.johnchourp.learnbyzantinemusic.summary_theory.ui.TESTIMONY_ROW
import com.johnchourp.learnbyzantinemusic.trainer.TrainerPhthong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Where the phthong analysis assumes a melody starts, for all eight modes (ClickUp `869f5x299`; the
 * old test skipped Β΄). Three things that used to be written separately must agree: the start
 * phthong, the mode's martyria, and the column of the «Μαρτυρίες» page that shows the mode.
 */
class AnalysisStartPhthongTest {

    @Test
    fun theAnalysisStartsWhereItAlwaysDid() {
        // Unchanged by the refactor: Α΄/Β΄ Πα, Γ΄ Γα, Δ΄ Βου, πλ. Α΄ Κε, πλ. Β΄ Δι, Βαρύς Ζω, πλ. Δ΄ Νη.
        val expected = mapOf(
            Mode.FIRST to TrainerPhthong.PA,
            Mode.SECOND to TrainerPhthong.PA,
            Mode.THIRD to TrainerPhthong.GA,
            Mode.FOURTH to TrainerPhthong.VOU,
            Mode.PLAGAL_FIRST to TrainerPhthong.KE,
            Mode.PLAGAL_SECOND to TrainerPhthong.DI,
            Mode.VARYS to TrainerPhthong.ZO,
            Mode.PLAGAL_FOURTH to TrainerPhthong.NI,
        )
        Mode.entries.forEach { mode ->
            assertEquals(mode.key, expected.getValue(mode), ModeScalePositions.defaultStartPhthong(mode))
        }
    }

    @Test
    fun theStartIsTheMartyria() {
        Mode.entries.forEach { mode ->
            assertEquals(mode.key, mode.martyria.name, ModeScalePositions.defaultStartPhthong(mode).name)
        }
    }

    @Test
    fun theMartyriaIsTheColumnOfTheTestimoniesPageThatShowsTheMode() {
        Mode.entries.forEach { mode ->
            val column = TESTIMONY_ROW.indexOfFirst { martyria ->
                martyria.modes.any { it.descRes == ModeResources.martyriaDescriptionRes(mode) }
            }
            assertTrue("${mode.key} has a badge on the page", column >= 0)
            // The row is Νη Πα Βου Γα Δι Κε Ζω Νη΄, so column c shows the c-th φθόγγος of the octave.
            assertEquals(mode.key, mode.martyria, PhthongName.entries[column % PhthongName.entries.size])
        }
    }
}
