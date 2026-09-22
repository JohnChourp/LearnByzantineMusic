package com.johnchourp.learnbyzantinemusic.modes.ui

import com.johnchourp.learnbyzantinemusic.modes.EightModeScaleDefinitions
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ClickUp `869f4tpju`: *«TalkBack διαβάζει όνομα φθόγγου και μόρια, όχι “image”»*.
 *
 * The μόρια are the information the diagram exists to convey — a sighted reader takes them from the
 * cell heights. Without them a screen-reader user gets a list of names and none of the intervals,
 * which is the entire lesson.
 *
 * Distances are measured from the nearest base **below**, because that is how the interval is
 * taught: «Δι, 30 μόρια πάνω από τη βάση» places the φθόγγος inside its own octave, where a distance
 * from the bottom of a three-octave ladder would exceed 72 and mean little.
 */
class ScaleDiagramAccessibilityTest {

    @Test
    fun theBaseItselfReportsZeroAndEachStepAddsItsInterval() {
        // Ladder top → bottom: 4 φθόγγοι, 3 gaps. The base is the bottom one.
        val intervals = listOf(10, 8, 12)
        val result = moriaFromNearestBaseBelow(intervals, setOf(3))
        assertEquals(listOf(30, 20, 12, 0), result)
    }

    @Test
    fun aPhthongBelowEveryBaseHasNoDistanceRatherThanAWrongOne() {
        // Nothing below it to measure from — the description must fall back to the name alone.
        val result = moriaFromNearestBaseBelow(listOf(10, 8, 12), setOf(0))
        assertEquals(listOf(0, null, null, null), result)
    }

    @Test
    fun eachOctaveIsMeasuredFromItsOwnBase() {
        // Two bases: everything resets at the lower one rather than accumulating across the ladder.
        val intervals = listOf(12, 10, 12, 10)
        val result = moriaFromNearestBaseBelow(intervals, setOf(2, 4))
        assertEquals(listOf(22, 10, 0, 10, 0), result)
    }

    @Test
    fun aLadderWithNoMarkedBaseReportsNothingRatherThanGuessing() {
        val result = moriaFromNearestBaseBelow(listOf(10, 8), emptySet())
        assertEquals(listOf(null, null, null), result)
    }

    @Test
    fun theResultAlwaysLinesUpWithTheLadderItDescribes() {
        // One entry per φθόγγος — a mismatch here would shift every spoken distance by one.
        listOf(listOf(10), listOf(10, 8, 12), listOf(6, 20, 4, 12, 6, 20, 4)).forEach { intervals ->
            assertEquals(
                intervals.size + 1,
                moriaFromNearestBaseBelow(intervals, setOf(intervals.size)).size,
            )
        }
    }

    @Test
    fun realModeLaddersProduceSensibleSpokenDistances() {
        val octaves = 3
        EightModeScaleDefinitions.MODE_SCALES.forEach { (key, scale) ->
            val phthongi = scale.ascendingPhthongi(octaves).reversed()
            val intervals = scale.repeatedIntervals(octaves).reversed()
            val tonic = phthongi.indices.filter { phthongi[it].name == scale.base.base.name }.toSet()
            val distances = moriaFromNearestBaseBelow(intervals, tonic)

            assertEquals("$key: one distance per φθόγγος", phthongi.size, distances.size)
            assertTrue("$key: the base must be marked", tonic.isNotEmpty())
            tonic.forEach { assertEquals("$key: a base is 0 from itself", 0, distances[it]) }

            // Inside one octave, so the number stays meaningful to a learner.
            distances.filterNotNull().forEach { moria ->
                assertTrue("$key: $moria μόρια is outside one octave", moria in 0..72)
            }
            // Only φθόγγοι below the lowest base may be unknown.
            val lowestBase = tonic.max()
            distances.forEachIndexed { index, value ->
                if (index <= lowestBase) assertTrue("$key index $index", value != null)
                else assertNull("$key index $index is below every base", value)
            }
        }
    }
}
