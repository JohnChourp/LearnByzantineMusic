package com.johnchourp.learnbyzantinemusic.music

import com.johnchourp.learnbyzantinemusic.modes.EightModeScaleDefinitions
import com.johnchourp.learnbyzantinemusic.modes.ModeScaleFrequencies
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ClickUp `869f4tpxj`: the typed ladder replaced three hand-rolled copies of the same computation on
 * the «8 Ήχοι» screen. The only acceptable outcome is that **nothing audible changed**.
 *
 * So the central test here is not "does the ladder look right" — it is that every pitch it produces
 * is **bit-for-bit** what the previous path produced, for every scale and every base shift the
 * slider can reach. A tolerance would hide precisely the kind of drift this refactor could
 * introduce, so the comparison is exact.
 */
class ModeLadderTest {

    private val octaves = 3
    private val reference = Phthong(PhthongName.NI)

    private val scales = listOf(
        "DIATONIC" to EightModeScaleDefinitions.DIATONIC,
        "SOFT_CHROMATIC" to EightModeScaleDefinitions.SOFT_CHROMATIC,
        "HARD_CHROMATIC" to EightModeScaleDefinitions.HARD_CHROMATIC,
        "ENHARMONIC" to EightModeScaleDefinitions.ENHARMONIC,
    )

    @Test
    fun theLadderSoundsExactlyWhatTheOldPathSounded() {
        var compared = 0
        scales.forEach { (name, scale) ->
            BaseShift.RANGE.forEach { shift ->
                val legacy = ModeScaleFrequencies.topToBottom(
                    ascendingIntervals = scale.repeatedIntervals(octaves),
                    referenceMoriaFromBottom = scale.referenceMoriaFromBottom("Νη", octaves),
                    baseShiftMoria = shift,
                )
                val ladder = scale.ladder(octaves, reference, Moria(shift))
                assertEquals("$name shift=$shift: rung count", legacy.size, ladder.steps.size)
                legacy.indices.forEach { i ->
                    assertEquals(
                        "$name shift=$shift rung $i",
                        legacy[i],
                        ladder.frequencies[i],
                        0.0, // exact: a tolerance would hide the drift this refactor could cause
                    )
                }
                compared++
            }
        }
        // Guards the sweep: an empty loop would pass without comparing anything. Since ClickUp
        // `869f5x2dd` the range is ±36 μόρια, 73 shifts per scale.
        assertEquals(scales.size * BaseShift.RANGE.count(), compared)
        assertEquals(73, BaseShift.RANGE.count())
    }

    @Test
    fun theLadderLabelsMatchTheOldLabelsInTheSameOrder() {
        scales.forEach { (name, scale) ->
            assertEquals(
                name,
                scale.ascendingPhthongs(octaves).reversed(),
                scale.ladder(octaves, reference).labels,
            )
        }
    }

    @Test
    fun theLadderRunsHighestFirst() {
        val ladder = EightModeScaleDefinitions.DIATONIC.ladder(octaves, reference)
        ladder.frequencies.zipWithNext().forEach { (higher, lower) ->
            assertTrue("$higher should be above $lower", higher > lower)
        }
    }

    @Test
    fun theReferenceNiSoundsAtTheReferenceFrequency() {
        scales.forEach { (name, scale) ->
            val step = scale.ladder(octaves, reference).stepFor(reference)
            assertNotNull("$name has no middle Νη", step)
            assertEquals(name, ByzantineTuning.NI_BASE_HZ, step!!.frequencyHz, 1e-9)
            assertEquals(Moria.ZERO, step.moriaFromNi)
        }
    }

    @Test
    fun aBaseShiftMovesEveryRungByExactlyThatManyMoria() {
        val scale = EightModeScaleDefinitions.DIATONIC
        val unshifted = scale.ladder(octaves, reference)
        listOf(BaseShift.MIN_MORIA, -12, -5, 5, 12, BaseShift.MAX_MORIA).forEach { shift ->
            val shifted = scale.ladder(octaves, reference, Moria(shift))
            unshifted.steps.indices.forEach { i ->
                assertEquals(
                    "shift=$shift rung $i",
                    unshifted.steps[i].moriaFromNi + Moria(shift),
                    shifted.steps[i].moriaFromNi,
                )
            }
        }
    }

    @Test
    fun stepForMatchesTheOctaveAndNotJustTheName() {
        // The bug a label comparison invites: "Πα" appearing four times and the wrong one winning.
        val ladder = EightModeScaleDefinitions.DIATONIC.ladder(octaves, reference)
        val middlePa = ladder.stepFor(Phthong(PhthongName.PA, 0))
        val upperPa = ladder.stepFor(Phthong(PhthongName.PA, 1))
        assertNotNull(middlePa)
        assertNotNull(upperPa)
        assertEquals(
            "one octave apart means exactly double",
            middlePa!!.frequencyHz * 2.0,
            upperPa!!.frequencyHz,
            1e-9,
        )
    }

    @Test
    fun aPhthongOutsideTheLadderIsRefusedRatherThanApproximated() {
        val ladder = EightModeScaleDefinitions.DIATONIC.ladder(octaves, reference)
        assertNull(ladder.stepFor(Phthong(PhthongName.PA, 9)))
    }

    @Test
    fun aMismatchedIntervalCountIsRejectedAtConstruction() {
        val phthongi = listOf(Phthong(PhthongName.NI), Phthong(PhthongName.PA))
        val error = runCatching {
            ModeLadder.build(phthongi, listOf(Moria(12), Moria(10)), Moria.ZERO)
        }.exceptionOrNull()
        assertTrue("expected a clear failure, got $error", error is IllegalArgumentException)
    }

    @Test
    fun theTonicIsMarkedInEveryOctaveJustAsTheLabelTrimUsedTo() {
        // The diagram highlights the base in every octave. The old code trimmed suffixes off labels;
        // the new one compares the typed name. These must select exactly the same rungs.
        scales.forEach { (name, scale) ->
            val ladder = scale.ladder(octaves, reference)
            val byTypedName = ladder.steps.indices.filter {
                ladder.steps[it].phthong.name == scale.base.base.name
            }
            val byLabelTrim = ladder.labels.indices.filter {
                ladder.labels[it].trimEnd('΄', ',') == scale.base.phthong
            }
            assertEquals(name, byLabelTrim, byTypedName)
            assertTrue("$name marks no tonic at all", byTypedName.isNotEmpty())
        }
    }
}
