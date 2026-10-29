package com.johnchourp.learnbyzantinemusic.recordings.analysis

import com.johnchourp.learnbyzantinemusic.docs.KotlinSource
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import com.johnchourp.learnbyzantinemusic.music.PhthongName.DI
import com.johnchourp.learnbyzantinemusic.music.PhthongName.GA
import com.johnchourp.learnbyzantinemusic.music.PhthongName.KE
import com.johnchourp.learnbyzantinemusic.music.PhthongName.PA
import com.johnchourp.learnbyzantinemusic.music.PhthongName.VOU
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The comparison carries the lists its alignment indexes (ClickUp `869f4tqef`). Before, the screen
 * read the steps against the state's expected melody, which is newer than the alignment for as long
 * as the recompute runs: after «Καθαρισμός» or backspace a step pointed past its end.
 */
class MelodyComparisonTest {

    private fun sung(vararg phthongs: PhthongName): List<SungNote> =
        phthongs.mapIndexed { i, p -> SungNote(p, octave = 0, startMs = i * 500L, endMs = i * 500L + 400, deviationMoria = 0.0, moria = 0.0) }

    @Test
    fun nothingToCompareWithoutAMelodyOrNotes() {
        assertNull(MelodyComparison.of(emptyList(), sung(PA)))
        assertNull(MelodyComparison.of(listOf(PA), emptyList()))
    }

    @Test
    fun everyStepPointsIntoTheComparisonsOwnLists() {
        val cases = listOf(
            listOf(PA, VOU, GA) to sung(PA, VOU, GA),
            listOf(PA, VOU, GA, DI) to sung(PA),
            listOf(PA) to sung(PA, KE, GA, DI),
            listOf(PA, VOU) to sung(KE, DI, GA),
        )
        for ((expected, notes) in cases) {
            val comparison = MelodyComparison.of(expected, notes)!!
            assertEquals(expected, comparison.expected)
            assertEquals(notes, comparison.notes)
            for (step in comparison.alignment.steps) {
                val (e, s) = when (step) {
                    is AlignmentStep.Match -> step.expected to step.sung
                    is AlignmentStep.Substitution -> step.expected to step.sung
                    is AlignmentStep.Missing -> step.expected to null
                    is AlignmentStep.Extra -> null to step.sung
                }
                e?.let { assertTrue("$step outside $expected", it in comparison.expected.indices) }
                s?.let { assertTrue("$step outside the sung notes", it in comparison.notes.indices) }
            }
        }
    }

    /** No JVM test can run the composable, so this reads it: a step index goes only into the comparison. */
    @Test
    fun theScreenReadsStepsOnlyThroughTheComparison() {
        val screen = File(KotlinSource.mainRoot, "com/johnchourp/learnbyzantinemusic/recordings/analysis/ui/RecordingAnalysisScreen.kt")
        val code = KotlinSource.withoutComments(screen.readText())
        val reads = Regex("""([\w.]+)\[step\.(expected|sung)]""").findAll(code).map { it.groupValues[1] }.toList()
        assertTrue("the screen reads no step at all", reads.isNotEmpty())
        assertEquals(emptyList<String>(), reads.filterNot { it == "comparison.expected" || it == "comparison.notes" })
    }
}
