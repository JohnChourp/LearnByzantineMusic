package com.johnchourp.learnbyzantinemusic.music

import com.johnchourp.learnbyzantinemusic.modes.EightModeScaleDefinitions
import com.johnchourp.learnbyzantinemusic.music.PhthongName.DI
import com.johnchourp.learnbyzantinemusic.music.PhthongName.GA
import com.johnchourp.learnbyzantinemusic.music.PhthongName.KE
import com.johnchourp.learnbyzantinemusic.music.PhthongName.NI
import com.johnchourp.learnbyzantinemusic.music.PhthongName.PA
import com.johnchourp.learnbyzantinemusic.music.PhthongName.VOU
import com.johnchourp.learnbyzantinemusic.music.PhthongName.ZO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The typed [Mode] as the one source of the eight modes (ClickUp `869f5x299`): its own facts and its
 * parse boundary. The facts stored on users' devices are pinned where they are used —
 * `EightModeUiModelsTest` for the keys, `StoredModeFolderNamesTest` for the folder names.
 */
class ModeTest {

    @Test
    fun theEntriesAreTheLiturgicalCycleNumberedOneToEight() {
        assertEquals(
            listOf(
                Mode.FIRST, Mode.SECOND, Mode.THIRD, Mode.FOURTH,
                Mode.PLAGAL_FIRST, Mode.PLAGAL_SECOND, Mode.VARYS, Mode.PLAGAL_FOURTH,
            ),
            Mode.entries.toList(),
        )
        assertEquals((1..8).toList(), Mode.entries.map { it.number })
    }

    @Test
    fun theCalendarsToneIndexIsTheNumberMinusOne() {
        (0..7).forEach { toneIndex ->
            assertEquals("tone index $toneIndex", toneIndex + 1, Mode.ofToneIndex(toneIndex).number)
        }
        assertThrows(IllegalArgumentException::class.java) { Mode.ofToneIndex(8) }
        assertThrows(IllegalArgumentException::class.java) { Mode.ofToneIndex(-1) }
    }

    @Test
    fun theMartyriaOfEveryModeIsTheOneTheAppAlwaysUsed() {
        // Α΄/Β΄ Πα, Γ΄ Γα, Δ΄ Βου, πλ. Α΄ Κε, πλ. Β΄ Δι, Βαρύς Ζω, πλ. Δ΄ Νη — unchanged by the refactor.
        assertEquals(listOf(PA, PA, GA, VOU, KE, DI, ZO, NI), Mode.entries.map { it.martyria })
    }

    @Test
    fun theScaleIsTheEntryOfTheOneModeToScaleTable() {
        Mode.entries.forEach { mode ->
            assertSame(mode.key, EightModeScaleDefinitions.SCALE_BY_MODE.getValue(mode), mode.scale)
        }
    }

    @Test
    fun anUnknownKeyIsNullAtTheBoundaryAndAnErrorWhereTheAppProducedIt() {
        Mode.entries.forEach { mode ->
            assertEquals(mode, Mode.fromKey(mode.key))
            assertEquals(mode, Mode.of(mode.key))
        }
        listOf(null, "", "xyz", "First", " first", "first ", "plagal fourth").forEach { key ->
            assertNull("«$key» names no mode", Mode.fromKey(key))
        }
        val error = assertThrows(IllegalArgumentException::class.java) { Mode.of("xyz") }
        assertTrue("the message names the key: ${error.message}", error.message.orEmpty().contains("xyz"))
    }
}
