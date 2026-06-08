package com.johnchourp.learnbyzantinemusic.modes

import org.junit.Assert.assertEquals
import org.junit.Test

class EightModeScaleDefinitionsTest {
    @Test
    fun `mode scale catalog uses corrected genera bases and intervals`() {
        assertScale(
            modeId = "first",
            expectedGenus = ModeScaleGenus.DIATONIC,
            expectedBase = ModeScaleBase.PA,
            expectedIntervals = listOf(10, 8, 12, 12, 10, 8, 12)
        )
        assertScale(
            modeId = "second",
            expectedGenus = ModeScaleGenus.SOFT_CHROMATIC,
            expectedBase = ModeScaleBase.PA,
            expectedIntervals = listOf(6, 20, 4, 12, 6, 20, 4)
        )
        assertScale(
            modeId = "third",
            expectedGenus = ModeScaleGenus.ENHARMONIC,
            expectedBase = ModeScaleBase.NI,
            expectedIntervals = listOf(12, 12, 6, 12, 12, 6, 12)
        )
        assertScale(
            modeId = "fourth",
            expectedGenus = ModeScaleGenus.DIATONIC,
            expectedBase = ModeScaleBase.PA,
            expectedIntervals = listOf(10, 8, 12, 12, 10, 8, 12)
        )
        assertScale(
            modeId = "plagal_first",
            expectedGenus = ModeScaleGenus.DIATONIC,
            expectedBase = ModeScaleBase.PA,
            expectedIntervals = listOf(10, 8, 12, 12, 10, 8, 12)
        )
        assertScale(
            modeId = "plagal_second",
            expectedGenus = ModeScaleGenus.HARD_CHROMATIC,
            expectedBase = ModeScaleBase.NI,
            expectedIntervals = listOf(8, 14, 8, 12, 8, 14, 8)
        )
        assertScale(
            modeId = "varys",
            expectedGenus = ModeScaleGenus.ENHARMONIC,
            expectedBase = ModeScaleBase.NI,
            expectedIntervals = listOf(12, 12, 6, 12, 12, 6, 12)
        )
        assertScale(
            modeId = "plagal_fourth",
            expectedGenus = ModeScaleGenus.DIATONIC,
            expectedBase = ModeScaleBase.PA,
            expectedIntervals = listOf(10, 8, 12, 12, 10, 8, 12)
        )
    }

    @Test
    fun `three octave labels are base aware`() {
        val paBasedLabels = EightModeScaleDefinitions.DIATONIC.ascendingPhthongs(octaves = 3)
        assertEquals(22, paBasedLabels.size)
        assertEquals("Πα,", paBasedLabels.first())
        assertEquals("Πα΄΄", paBasedLabels.last())
        assertEquals(listOf("Πα,", "Βου,", "Γα,", "Δι,", "Κε,", "Ζω,", "Νη", "Πα"), paBasedLabels.take(8))

        val niBasedLabels = EightModeScaleDefinitions.ENHARMONIC.ascendingPhthongs(octaves = 3)
        assertEquals(22, niBasedLabels.size)
        assertEquals("Νη,", niBasedLabels.first())
        assertEquals("Νη΄΄", niBasedLabels.last())
        assertEquals(listOf("Νη,", "Πα,", "Βου,", "Γα,", "Δι,", "Κε,", "Ζω,", "Νη"), niBasedLabels.take(8))
    }

    @Test
    fun `frequency reference remains unmarked ni for each scale`() {
        assertEquals(60, EightModeScaleDefinitions.DIATONIC.referenceMoriaFromBottom("Νη", octaves = 3))
        assertEquals(68, EightModeScaleDefinitions.SOFT_CHROMATIC.referenceMoriaFromBottom("Νη", octaves = 3))
        assertEquals(72, EightModeScaleDefinitions.HARD_CHROMATIC.referenceMoriaFromBottom("Νη", octaves = 3))
        assertEquals(72, EightModeScaleDefinitions.ENHARMONIC.referenceMoriaFromBottom("Νη", octaves = 3))
    }

    private fun assertScale(
        modeId: String,
        expectedGenus: ModeScaleGenus,
        expectedBase: ModeScaleBase,
        expectedIntervals: List<Int>
    ) {
        val scale = EightModeScaleDefinitions.MODE_SCALES[modeId] ?: error("Missing mode $modeId")
        assertEquals(expectedGenus, scale.genus)
        assertEquals(expectedBase, scale.base)
        assertEquals(expectedIntervals, scale.intervals)
    }
}
