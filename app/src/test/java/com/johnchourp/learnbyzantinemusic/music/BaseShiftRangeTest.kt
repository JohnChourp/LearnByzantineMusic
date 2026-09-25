package com.johnchourp.learnbyzantinemusic.music

import com.johnchourp.learnbyzantinemusic.docs.KotlinSource
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs
import com.johnchourp.learnbyzantinemusic.trainer.TrainerScale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The «Μεταφορά βάσης» range, declared once in [BaseShift] (ClickUp `869f5x24v`, F2).
 *
 * Since F2 the Melody Trainer and the 8 Ήχοι page read and write the **same** per-mode key. If one
 * of them clamped to a wider range than the other, a value set on one screen would be cut back,
 * silently, the next time the other read it. The 8 Ήχοι screen and activity still carry their own
 * copies of the range; until ClickUp `869f5x2dd` (J4) folds them into [BaseShift], this test is
 * what keeps them equal — change one and it names the others.
 */
class BaseShiftRangeTest {

    @Test
    fun theRangeIsTwelveMoriaEitherWay() {
        assertEquals(-12, BaseShift.MIN_MORIA)
        assertEquals(12, BaseShift.MAX_MORIA)
        assertEquals(0, BaseShift.DEFAULT_MORIA)
        assertEquals(-12..12, BaseShift.RANGE)
    }

    @Test
    fun clampingBringsAnyValueInside() {
        assertEquals(-12, BaseShift.clamp(-40))
        assertEquals(12, BaseShift.clamp(40))
        assertEquals(-5, BaseShift.clamp(-5))
    }

    /** The copies still living in the 8 Ήχοι code: each must be found, once, and equal [BaseShift]. */
    @Test
    fun theEightModesCopiesAgreeWithTheSharedRange() {
        val modes = File(KotlinSource.mainRoot, "com/johnchourp/learnbyzantinemusic/modes")
        val copies = mapOf(
            "EightModesActivity.kt" to ("BASE_SHIFT_MORIA_MIN" to "BASE_SHIFT_MORIA_MAX"),
            "ui/EightModesScreen.kt" to ("BASE_SHIFT_MIN" to "BASE_SHIFT_MAX"),
        )
        copies.forEach { (path, names) ->
            val code = KotlinSource.withoutComments(File(modes, path).readText())
            assertEquals("$path ${names.first}", BaseShift.MIN_MORIA, declaredInt(code, names.first, path))
            assertEquals("$path ${names.second}", BaseShift.MAX_MORIA, declaredInt(code, names.second, path))
        }
        // The registry documents the same range for the shared key.
        val documented = AppPrefs.BaseShiftMoria.allowed.orEmpty()
        assertTrue(documented, documented.startsWith("${BaseShift.MIN_MORIA}..+${BaseShift.MAX_MORIA} "))
    }

    @Test
    fun theTrainerRefusesAShiftTheRangeRefuses() {
        assertThrows(IllegalArgumentException::class.java) { TrainerScale(Mode.FIRST, BaseShift.MAX_MORIA + 1) }
        assertThrows(IllegalArgumentException::class.java) { TrainerScale(Mode.FIRST, BaseShift.MIN_MORIA - 1) }
        // «Διατονικός» is the fixed default: it has no shift of its own to store.
        assertThrows(IllegalArgumentException::class.java) { TrainerScale(null, 1) }
    }

    private fun declaredInt(code: String, name: String, path: String): Int {
        val matches = Regex("""\b${name}\s*=\s*(-?\d+)\b""").findAll(code).toList()
        assertEquals("$name must be declared exactly once in $path", 1, matches.size)
        return matches.single().groupValues[1].toInt()
    }
}
