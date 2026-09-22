package com.johnchourp.learnbyzantinemusic.music

import com.johnchourp.learnbyzantinemusic.modes.EightModeScaleDefinitions
import com.johnchourp.learnbyzantinemusic.modes.ModeScaleBase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The safety net for ClickUp `869f4tpxj`'s hardest criterion: *«Τα εμφανιζόμενα ονόματα παραμένουν
 * ακριβώς ίδια (EL+EN) — καθαρά εσωτερική αλλαγή»*.
 *
 * A typing refactor is allowed to change how a label is computed and nothing about what it says. So
 * this file reimplements the **pre-refactor** decoration algorithm from scratch — the suffix counter
 * that incremented on Νη, and the plain rotation the interval table used — and asserts the typed
 * model renders byte-identically to it.
 *
 * It earned its place immediately: the first version of the refactor implemented the interval table
 * from the ladder, which turned a Πα-based run's `Νη` into `Νη΄`. No existing test covered those
 * functions, so the suite stayed green and the change would have shipped invisibly.
 */
class PhthongRenderingParityTest {

    private val order = listOf("Νη", "Πα", "Βου", "Γα", "Δι", "Κε", "Ζω")

    /** The old `decoratePhthong`, verbatim in behaviour. */
    private fun legacyDecorate(name: String, suffixLevel: Int): String = name + when {
        suffixLevel < 0 -> ","
        suffixLevel == 0 -> ""
        else -> "΄".repeat(suffixLevel)
    }

    /** The old `ascendingPhthongs`: a suffix counter bumped whenever the run passes Νη. */
    private fun legacyAscending(basePhthong: String, octaves: Int): List<String> {
        var suffixLevel = -1
        var index = order.indexOf(basePhthong)
        val labels = mutableListOf(legacyDecorate(order[index], suffixLevel))
        repeat(order.size * octaves) {
            index = (index + 1) % order.size
            val name = order[index]
            if (name == "Νη") suffixLevel += 1
            labels.add(legacyDecorate(name, suffixLevel))
        }
        return labels
    }

    /** The old `singleOctavePhthongs`: a plain rotation, then the base one octave up. */
    private fun legacySingleOctave(basePhthong: String): List<String> {
        val start = order.indexOf(basePhthong)
        return (order.drop(start) + order.take(start)) + "$basePhthong΄"
    }

    @Test
    fun theLadderRendersExactlyAsItDidForEveryBaseAndOctaveCount() {
        var compared = 0
        ModeScaleBase.entries.forEach { base ->
            (1..4).forEach { octaves ->
                assertEquals(
                    "base=${base.phthong} octaves=$octaves",
                    legacyAscending(base.phthong, octaves),
                    EightModeScaleDefinitions.ascendingPhthongs(base, octaves),
                )
                compared++
            }
        }
        // Guards the sweep: an empty loop would make this pass without comparing anything.
        assertEquals(ModeScaleBase.entries.size * 4, compared)
        assertTrue(compared >= 8)
    }

    @Test
    fun theIntervalTableStillLabelsItsMiddleNiWithoutAnOctaveMark() {
        // The exact regression the first refactor introduced.
        ModeScaleBase.entries.forEach { base ->
            assertEquals(
                "base=${base.phthong}",
                legacySingleOctave(base.phthong),
                EightModeScaleDefinitions.singleOctavePhthongs(base),
            )
        }
        assertEquals(
            listOf("Πα", "Βου", "Γα", "Δι", "Κε", "Ζω", "Νη", "Πα΄"),
            EightModeScaleDefinitions.singleOctavePhthongs(ModeScaleBase.PA),
        )
    }

    @Test
    fun theLadderAndTheIntervalTableGenuinelyDisagreeAboutThatNi() {
        // Pins the distinction itself, so nobody "simplifies" one into the other. The ladder is
        // octave-aware because its φθόγγοι are sounded; the table labels steps.
        val ladderOctave = EightModeScaleDefinitions.ascendingPhthongs(ModeScaleBase.PA, 1)
        val table = EightModeScaleDefinitions.singleOctavePhthongs(ModeScaleBase.PA)
        assertTrue("the ladder marks its Νη", ladderOctave.any { it == "Νη" || it == "Νη΄" })
        assertTrue("the table leaves its Νη bare", "Νη" in table)
        assertTrue("the table has no marked Νη", table.none { it == "Νη΄" })
    }

    @Test
    fun upperBaseIsUnchanged() {
        assertEquals("Πα΄", EightModeScaleDefinitions.DIATONIC.upperBase)
        assertEquals("Νη΄", EightModeScaleDefinitions.ENHARMONIC.upperBase)
    }

    @Test
    fun intervalSummaryIsUnchanged() {
        assertEquals(
            "Πα-Βου 10, Βου-Γα 8, Γα-Δι 12, Δι-Κε 12, Κε-Ζω 10, Ζω-Νη 8, Νη-Πα΄ 12",
            EightModeScaleDefinitions.DIATONIC.intervalSummary(),
        )
    }

    @Test
    fun everyLadderLabelParsesBackToTheFphthongThatRenderedIt() {
        // Round-tripping is what lets the boundary parser be trusted at the edges.
        ModeScaleBase.entries.forEach { base ->
            EightModeScaleDefinitions.ascendingPhthongi(base, 3).forEach { phthong ->
                assertEquals(phthong, Phthong.parse(phthong.label))
            }
        }
    }
}
