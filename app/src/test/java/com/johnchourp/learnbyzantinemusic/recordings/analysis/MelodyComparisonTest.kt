package com.johnchourp.learnbyzantinemusic.recordings.analysis

import com.johnchourp.learnbyzantinemusic.docs.KotlinSource
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import com.johnchourp.learnbyzantinemusic.music.PhthongName.DI
import com.johnchourp.learnbyzantinemusic.music.PhthongName.GA
import com.johnchourp.learnbyzantinemusic.music.PhthongName.KE
import com.johnchourp.learnbyzantinemusic.music.PhthongName.NI
import com.johnchourp.learnbyzantinemusic.music.PhthongName.PA
import com.johnchourp.learnbyzantinemusic.music.PhthongName.VOU
import com.johnchourp.learnbyzantinemusic.music.PhthongName.ZO
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
            assertEquals("the score's «από Y»", expected.size, comparison.alignment.expectedCount)
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

    /** «Πα → Γα, 2 σκαλιά πάνω» (ClickUp `1amndjnkdqy`): the nearer way round, never more than 3. */
    @Test
    fun scaleStepsTakeTheNearerWayRound() {
        assertEquals(2, MelodyComparison.scaleSteps(PA, GA))
        assertEquals(-2, MelodyComparison.scaleSteps(GA, PA))
        assertEquals(-2, MelodyComparison.scaleSteps(PA, ZO))
        assertEquals(1, MelodyComparison.scaleSteps(ZO, NI))
        assertEquals(3, MelodyComparison.scaleSteps(PA, DI))
        assertEquals(-3, MelodyComparison.scaleSteps(PA, KE))
        for (from in PhthongName.entries) {
            for (to in PhthongName.entries) {
                val steps = MelodyComparison.scaleSteps(from, to)
                assertTrue("$from → $to: $steps", steps in -3..3)
                assertEquals("$from → $to lands on $to", to.ordinal, Math.floorMod(from.ordinal + steps, 7))
                assertEquals("$from → $to and back", -steps, MelodyComparison.scaleSteps(to, from))
                if (from == to) assertEquals(0, steps)
            }
        }
    }

    @Test
    fun aSubstitutionSaysHowFarOff() {
        // Expected Πα Βου Γα; sung Πα, Δι (for Βου), Γα, Ζω (extra).
        val comparison = MelodyComparison.of(listOf(PA, VOU, GA), sung(PA, DI, GA, ZO))!!
        val wrong = comparison.alignment.steps.filterIsInstance<AlignmentStep.Substitution>().single()
        assertEquals(2, comparison.stepsOff(wrong))
    }

    @Test
    fun aNoteSungLowerIsStepsDown() {
        val comparison = MelodyComparison.of(listOf(GA), sung(NI))!!
        val wrong = comparison.alignment.steps.single() as AlignmentStep.Substitution
        assertEquals(-3, comparison.stepsOff(wrong))
    }

    /**
     * No JVM test can run the composables, so this reads them: in every file of the analysis UI, a step
     * index goes only into the comparison's lists, whether as `list[i]` or `list.get(i)` and its kin.
     */
    @Test
    fun theScreenReadsStepsOnlyThroughTheComparison() {
        val ui = File(KotlinSource.mainRoot, "com/johnchourp/learnbyzantinemusic/recordings/analysis/ui")
        val read = Regex("""([\w.]+?)(?:\[|\.(?:get|getOrNull|getOrElse|elementAt|elementAtOrNull)\()step\.(expected|sung)\b""")
        val reads = ui.listFiles { f -> f.extension == "kt" }.orEmpty().flatMap { file ->
            read.findAll(KotlinSource.withoutComments(file.readText())).map { "${file.name}: ${it.groupValues[1]}" }.toList()
        }
        assertTrue("the analysis UI reads no step at all", reads.isNotEmpty())
        assertEquals(emptyList<String>(), reads.filterNot { it.endsWith(": comparison.expected") || it.endsWith(": comparison.notes") })
    }
}
