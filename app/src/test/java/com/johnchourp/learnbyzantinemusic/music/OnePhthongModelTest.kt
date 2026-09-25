package com.johnchourp.learnbyzantinemusic.music

import com.johnchourp.learnbyzantinemusic.lessons.ui.PhthongScale
import com.johnchourp.learnbyzantinemusic.modes.EightModeScaleDefinitions
import com.johnchourp.learnbyzantinemusic.recordings.analysis.PhthongSegmenter
import com.johnchourp.learnbyzantinemusic.recordings.analysis.SungNote
import com.johnchourp.learnbyzantinemusic.trainer.TrainerNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * «Τα ονόματα των φθόγγων είναι ίδια σε όλες τις οθόνες» (ClickUp `869f5x291`, H2).
 *
 * There were three enums of the seven φθόγγοι — [PhthongName], the Melody Trainer's `TrainerPhthong`
 * (with its own copy of the Greek names) and the lessons' `Phthong` — and two screens rendered their
 * own labels. Now there is one enum, and every screen that shows a φθόγγος at an octave goes through
 * [Phthong.label]. Both halves are checked: the declaration by a source scan, the rendering by asking
 * each screen's own model for the same pitch.
 */
class OnePhthongModelTest {

    private val mainSources: List<File> by lazy {
        val root = listOf(File("app/src/main/java"), File("src/main/java")).firstOrNull { it.isDirectory }
            ?: error("Cannot locate the main source root from ${File("").absolutePath}")
        root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    /**
     * An enum whose first constant is NI: a list of the seven φθόγγοι. Anchored to the start of a
     * line, so a commented-out declaration (`//` or ` * `) is not one.
     */
    private val phthongEnum = Regex("""(?m)^[ \t]*(?:(?:public|internal|private)\s+)?enum\s+class\s+(\w+)[^{]*\{\s*NI\b""")

    private fun enumsIn(text: String): List<String> = phthongEnum.findAll(text).map { it.groupValues[1] }.toList()

    @Test
    fun onlyOneEnumListsTheSevenPhthongs() {
        assertTrue("expected to scan real sources", mainSources.size > 50)
        // PhthongName itself is the positive control: a pattern that matched nothing would fail here.
        assertEquals(listOf("PhthongName"), mainSources.flatMap { enumsIn(it.readText()) })
    }

    @Test
    fun theScanWouldHaveCaughtTheTwoEnumsH2Removed() {
        // Their declarations as they stood, up to the first constant.
        val before = """
            enum class TrainerPhthong(val displayName: String, val diatonicMoriaFromNi: Int) {
                NI("Νη", 0),
            enum class Phthong(val sourceLetter: Char) {
                NI('Η'),
        """.trimIndent()
        assertEquals(listOf("TrainerPhthong", "Phthong"), enumsIn(before))
        assertEquals("a commented-out enum is not one", emptyList<String>(), enumsIn("// enum class Old {\n//     NI,\n"))
    }

    @Test
    fun everyScreenRendersTheSamePitchTheSameWay() {
        // The reference is the 8 Ήχοι diagram's own labels: three octaves of the diatonic ladder.
        val ladder = EightModeScaleDefinitions.DIATONIC.ladder(octaves = 3)
        assertTrue("expected a real ladder, got ${ladder.steps.size} rungs", ladder.steps.size >= 22)
        ladder.steps.forEachIndexed { index, step ->
            val pitch = step.phthong
            val diagram = ladder.labels[index]
            assertEquals("Trainer, $diagram", diagram, TrainerNote(pitch.name, octaveShift = pitch.octave).pitch.label)
            assertEquals(
                "analysis lines, $diagram",
                diagram,
                PhthongSegmenter.phthongAt(pitch.octave * 7 + pitch.name.ordinal).label,
            )
            val sung = SungNote(pitch.name, pitch.octave, startMs = 0, endMs = 500, deviationMoria = 0.0, moria = 0.0)
            assertEquals("analysis notes, $diagram", diagram, sung.pitch.label)
        }
    }

    @Test
    fun theMarksAreOnePerDirection() {
        assertEquals("Ζω,", TrainerNote(PhthongName.ZO, octaveShift = -1).pitch.label)
        assertEquals("Δι,,", PhthongSegmenter.phthongAt(-2 * 7 + PhthongName.DI.ordinal).label)
        assertEquals("Νη΄", PhthongSegmenter.phthongAt(7).label)
        assertEquals("Πα΄΄", TrainerNote(PhthongName.PA, octaveShift = 2).pitch.label)
    }

    @Test
    fun theLessonsSpeakTheSameModel() {
        assertEquals(PhthongName.entries.toList(), PhthongScale.octave)
        assertEquals(PhthongName.entries.toSet(), PhthongScale.byAlphabet.toSet())
    }
}
