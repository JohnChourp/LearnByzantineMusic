package com.johnchourp.learnbyzantinemusic.recordings.analysis

import com.johnchourp.learnbyzantinemusic.modes.EightModeScaleDefinitions
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import com.johnchourp.learnbyzantinemusic.trainer.diatonicMoriaFromNi
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModeScalePositionsTest {

    @Test
    fun diatonicMatchesTheTrainerTable() {
        val expected = PhthongName.entries.map { it.diatonicMoriaFromNi }.toIntArray()
        assertArrayEquals(expected, ModeScalePositions.forMode("first"))
        assertArrayEquals(expected, ModeScalePositions.forMode("plagal_fourth"))
    }

    @Test
    fun chromaticAndEnharmonicPositions() {
        assertArrayEquals(intArrayOf(0, 8, 16, 30, 38, 50, 58), ModeScalePositions.forDefinition(EightModeScaleDefinitions.SOFT_CHROMATIC))
        assertArrayEquals(intArrayOf(0, 6, 26, 30, 42, 48, 68), ModeScalePositions.forDefinition(EightModeScaleDefinitions.HARD_CHROMATIC))
        assertArrayEquals(intArrayOf(0, 12, 24, 30, 42, 54, 60), ModeScalePositions.forDefinition(EightModeScaleDefinitions.ENHARMONIC))
    }

    @Test
    fun everyModeHasSevenAscendingPositionsWithinAnOctave() {
        for (modeKey in EightModeScaleDefinitions.MODE_SCALES.keys) {
            val positions = ModeScalePositions.forMode(modeKey)
            assertEquals(modeKey, 7, positions.size)
            assertEquals(modeKey, 0, positions[0])
            for (index in 1 until positions.size) {
                assertTrue(modeKey, positions[index] > positions[index - 1])
            }
            assertTrue(modeKey, positions.last() < ModeScalePositions.MORIA_PER_OCTAVE)
        }
    }

    @Test
    fun defaultStartPhthongsFollowTheModeSigns() {
        assertEquals(PhthongName.PA, ModeScalePositions.defaultStartPhthong("first"))
        assertEquals(PhthongName.GA, ModeScalePositions.defaultStartPhthong("third"))
        assertEquals(PhthongName.VOU, ModeScalePositions.defaultStartPhthong("fourth"))
        assertEquals(PhthongName.KE, ModeScalePositions.defaultStartPhthong("plagal_first"))
        assertEquals(PhthongName.DI, ModeScalePositions.defaultStartPhthong("plagal_second"))
        assertEquals(PhthongName.ZO, ModeScalePositions.defaultStartPhthong("varys"))
        assertEquals(PhthongName.NI, ModeScalePositions.defaultStartPhthong("plagal_fourth"))
    }
}
