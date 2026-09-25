package com.johnchourp.learnbyzantinemusic.recordings.analysis

import com.johnchourp.learnbyzantinemusic.modes.EightModeScaleDefinitions
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.trainer.TrainerPhthong
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModeScalePositionsTest {

    @Test
    fun diatonicMatchesTheTrainerTable() {
        val expected = TrainerPhthong.ascending.map { it.diatonicMoriaFromNi }.toIntArray()
        assertArrayEquals(expected, ModeScalePositions.forMode(Mode.FIRST))
        assertArrayEquals(expected, ModeScalePositions.forMode(Mode.PLAGAL_FOURTH))
    }

    @Test
    fun chromaticAndEnharmonicPositions() {
        assertArrayEquals(intArrayOf(0, 8, 16, 30, 38, 50, 58), ModeScalePositions.forDefinition(EightModeScaleDefinitions.SOFT_CHROMATIC))
        assertArrayEquals(intArrayOf(0, 6, 26, 30, 42, 48, 68), ModeScalePositions.forDefinition(EightModeScaleDefinitions.HARD_CHROMATIC))
        assertArrayEquals(intArrayOf(0, 12, 24, 30, 42, 54, 60), ModeScalePositions.forDefinition(EightModeScaleDefinitions.ENHARMONIC))
    }

    @Test
    fun everyModeHasSevenAscendingPositionsWithinAnOctave() {
        for (mode in Mode.entries) {
            val modeKey = mode.key
            val positions = ModeScalePositions.forMode(mode)
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
        assertEquals(TrainerPhthong.PA, ModeScalePositions.defaultStartPhthong(Mode.FIRST))
        assertEquals(TrainerPhthong.GA, ModeScalePositions.defaultStartPhthong(Mode.THIRD))
        assertEquals(TrainerPhthong.VOU, ModeScalePositions.defaultStartPhthong(Mode.FOURTH))
        assertEquals(TrainerPhthong.KE, ModeScalePositions.defaultStartPhthong(Mode.PLAGAL_FIRST))
        assertEquals(TrainerPhthong.DI, ModeScalePositions.defaultStartPhthong(Mode.PLAGAL_SECOND))
        assertEquals(TrainerPhthong.ZO, ModeScalePositions.defaultStartPhthong(Mode.VARYS))
        assertEquals(TrainerPhthong.NI, ModeScalePositions.defaultStartPhthong(Mode.PLAGAL_FOURTH))
    }
}
