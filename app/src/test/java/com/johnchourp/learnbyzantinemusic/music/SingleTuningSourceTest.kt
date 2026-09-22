package com.johnchourp.learnbyzantinemusic.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the "one named place" rule of ClickUp `869f4tq0p`.
 *
 * It is not enough that [ByzantineTuning] exists — the point is that nothing *else* declares the
 * reference frequency or the size of the octave, and that no other file re-derives a pitch with its
 * own `2.0.pow(... / 72 ...)`. Both mistakes compile, both keep every existing test green, and both
 * let the played pitch drift away from the listened-for pitch.
 *
 * Deliberately scoped:
 * - only *declarations* (`const val … = 220.0`) count, so `tween(220)` and `72.dp` in Compose layout
 *   code are not false positives;
 * - the power-of-two scan is limited to expressions dividing by 72, so the synth's cents maths
 *   (`2.0.pow(cents / 1200.0)`) stays legal.
 */
class SingleTuningSourceTest {

    private val tuningFile = "ByzantineTuning.kt"

    private val mainSources: List<File> by lazy {
        val roots = listOf(File("app/src/main/java"), File("src/main/java"))
        val root = roots.firstOrNull { it.isDirectory }
            ?: error("Cannot locate the main source root from ${File("").absolutePath}")
        root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    @Test
    fun theSourceRootIsActuallyFound() {
        // Without this, every assertion below would pass vacuously over an empty list.
        assertTrue("expected to scan real sources", mainSources.size > 50)
        assertTrue(
            "the tuning file itself must be among the scanned sources",
            mainSources.any { it.name == tuningFile }
        )
    }

    @Test
    fun onlyByzantineTuningDeclaresTheReferenceFrequency() {
        val declaration = Regex("""const\s+val\s+\w+\s*(:\s*\w+\s*)?=\s*220(\.0)?\b""")
        assertEquals(
            "220.0 must be declared once, in $tuningFile",
            listOf(tuningFile),
            mainSources.filter { declaration.containsMatchIn(it.readText()) }.map { it.name }.sorted()
        )
    }

    @Test
    fun onlyByzantineTuningDeclaresTheOctaveSize() {
        val declaration = Regex("""const\s+val\s+\w+\s*(:\s*\w+\s*)?=\s*72(\.0)?\b""")
        assertEquals(
            "72 must be declared once, in $tuningFile",
            listOf(tuningFile),
            mainSources.filter { declaration.containsMatchIn(it.readText()) }.map { it.name }.sorted()
        )
    }

    @Test
    fun noOtherFileRaisesTwoToAMoriaFraction() {
        val ownMaths = Regex("""2\.0\.pow\([^)]*(72|MORIA_PER_OCTAVE)[^)]*\)""")
        assertEquals(
            "the moria→Hz power belongs to $tuningFile only",
            listOf(tuningFile),
            mainSources.filter { ownMaths.containsMatchIn(it.readText()) }.map { it.name }.sorted()
        )
    }
}
