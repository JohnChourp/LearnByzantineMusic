package com.johnchourp.learnbyzantinemusic.music

import com.johnchourp.learnbyzantinemusic.trainer.diatonicMoriaFromNi
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [PhthongName] is stored data (ClickUp `869f5x291`, H2). The recording analysis writes the constant
 * **names** into its preferences and places each φθόγγος on a mode's scale by **ordinal**, so folding
 * the Melody Trainer's `TrainerPhthong` and the lessons' `Phthong` into this enum was allowed to change
 * neither — nor anything the two brought with them.
 */
class PhthongNamesAreFrozenTest {

    @Test
    fun theConstantNamesAreTheOnesAlreadyOnUsersDevices() {
        // Exactly the names TrainerPhthong wrote: "NI,PA,VOU" is a melody saved before H2.
        assertEquals(listOf("NI", "PA", "VOU", "GA", "DI", "KE", "ZO"), PhthongName.entries.map { it.name })
    }

    @Test
    fun theOrderAscendsFromNiAndEachOrdinalIsTheScaleIndex() {
        // ModeScalePositions arrays and PhthongSegmenter's degrees are indexed by ordinal.
        assertEquals((0..6).toList(), PhthongName.entries.map { it.ordinal })
        assertEquals(listOf("Νη", "Πα", "Βου", "Γα", "Δι", "Κε", "Ζω"), PhthongName.entries.map { it.displayName })
    }

    @Test
    fun theLessonsLettersCameAlongUnchanged() {
        assertEquals(listOf('Η', 'Α', 'Β', 'Γ', 'Δ', 'Ε', 'Ζ'), PhthongName.entries.map { it.sourceLetter })
    }

    @Test
    fun theTrainersDiatonicPositionsCameAlongUnchanged() {
        assertEquals(listOf(0, 12, 22, 30, 42, 54, 64), PhthongName.entries.map { it.diatonicMoriaFromNi })
    }
}
