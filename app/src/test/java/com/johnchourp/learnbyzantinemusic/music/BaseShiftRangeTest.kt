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
 * The «Μεταφορά βάσης» range, declared once in [BaseShift] (ClickUp `869f5x24v`, F2; `869f5x2dd`, J4).
 *
 * The Melody Trainer and the 8 Ήχοι page read and write the **same** per-mode key, and since J4 every
 * ladder also adds the voice's global shift. If any of them clamped to another range, a value set on
 * one screen would be cut back, silently, the next time another read it. J4 folded the 8 Ήχοι page's
 * own copies into [BaseShift] and widened the range to ±36 μόρια (owner decision, 2026-09-25); this
 * test pins the range, that there is one declaration of it, and the clamp of the combined shift.
 */
class BaseShiftRangeTest {

    @Test
    fun theRangeIsHalfAnOctaveEitherWay() {
        assertEquals(-36, BaseShift.MIN_MORIA)
        assertEquals(36, BaseShift.MAX_MORIA)
        assertEquals(0, BaseShift.DEFAULT_MORIA)
        assertEquals(-36..36, BaseShift.RANGE)
        // Half an octave each way — with octaves folded, a whole octave of voices is reachable.
        assertEquals(ByzantineTuning.MORIA_PER_OCTAVE, BaseShift.MAX_MORIA - BaseShift.MIN_MORIA)
    }

    @Test
    fun clampingBringsAnyValueInside() {
        assertEquals(-36, BaseShift.clamp(-40))
        assertEquals(36, BaseShift.clamp(40))
        assertEquals(-5, BaseShift.clamp(-5))
        // Every value saved before J4, within the old ±12, is read back exactly as it was saved.
        (-12..12).forEach { saved -> assertEquals(saved, BaseShift.clamp(saved)) }
    }

    @Test
    fun theCombinedShiftIsTheSumClampedAtThePointOfUse() {
        // A global 0 leaves every mode's own shift exactly as it is.
        BaseShift.RANGE.forEach { own -> assertEquals(own, BaseShift.combined(own, BaseShift.DEFAULT_MORIA)) }
        assertEquals(-5, BaseShift.combined(BaseShift.DEFAULT_MORIA, -5))
        assertEquals(19, BaseShift.combined(12, 7))
        assertEquals(0, BaseShift.combined(36, -36))
        // Past the range the sum is clamped — never wrapped, and never written back.
        assertEquals(36, BaseShift.combined(30, 20))
        assertEquals(-36, BaseShift.combined(-30, -20))
        assertEquals(36, BaseShift.combined(36, 36))
    }

    /** The copies J4 folded in stay gone, and no other file declares bounds of its own. */
    @Test
    fun theRangeIsDeclaredInOnePlaceOnly() {
        val main = KotlinSource.mainRoot
        val files = main.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        // A scan over too few files proves nothing.
        assertTrue("only ${files.size} Kotlin files found under $main", files.size > 100)
        val offenders = files.flatMap { file ->
            val path = file.relativeTo(main).invariantSeparatorsPath
            rangeDeclarations(KotlinSource.withoutComments(file.readText()))
                .filterNot { path == BASE_SHIFT_FILE && it in OWN_DECLARATIONS }
                .map { "$path: $it" }
        }
        assertEquals(emptyList<String>(), offenders)
        // ...while the one declaration is found where it belongs: the scan can see one.
        val own = rangeDeclarations(KotlinSource.withoutComments(File(main, BASE_SHIFT_FILE).readText()))
        assertEquals(OWN_DECLARATIONS, own.toSet())
        // The registry documents the same range for the shared key.
        val documented = AppPrefs.BaseShiftMoria.allowed.orEmpty()
        assertTrue(documented, documented.startsWith("${BaseShift.MIN_MORIA}..+${BaseShift.MAX_MORIA} "))
    }

    /** The detector itself, on the copies as they were before J4: each is seen. */
    @Test
    fun theScanRecognisesTheOldCopies() {
        val before = """
            private const val BASE_SHIFT_MORIA_MIN = -12
            private const val BASE_SHIFT_MORIA_MAX = 12
            internal const val BASE_SHIFT_MIN = -12
            internal const val BASE_SHIFT_MAX = 12
        """
        assertEquals(4, rangeDeclarations(before).size)
        // A comment that names one is not a declaration, and neither is a comparison.
        assertEquals(emptyList<String>(), rangeDeclarations(KotlinSource.withoutComments("// BASE_SHIFT_MIN = -12")))
        assertEquals(emptyList<String>(), rangeDeclarations("if (BaseShift.MIN_MORIA == shift) return"))
    }

    @Test
    fun theTrainerRefusesAShiftTheRangeRefuses() {
        assertThrows(IllegalArgumentException::class.java) { TrainerScale(Mode.FIRST, BaseShift.MAX_MORIA + 1) }
        assertThrows(IllegalArgumentException::class.java) { TrainerScale(Mode.FIRST, BaseShift.MIN_MORIA - 1) }
        assertThrows(IllegalArgumentException::class.java) {
            TrainerScale(Mode.FIRST, BaseShift.DEFAULT_MORIA, BaseShift.MAX_MORIA + 1)
        }
        // «Διατονικός» has no shift of its own to store — the voice's global one it does take.
        assertThrows(IllegalArgumentException::class.java) { TrainerScale(null, 1) }
        assertEquals(BaseShift.MIN_MORIA, TrainerScale(null, BaseShift.DEFAULT_MORIA, BaseShift.MIN_MORIA).ladderShiftMoria)
    }

    /** Every declaration of a base-shift bound in [code]: the old copies' names, or a MIN_/MAX_MORIA. */
    private fun rangeDeclarations(code: String): List<String> =
        DECLARATION.findAll(code).map { it.groupValues[1] }.toList()

    private companion object {
        const val BASE_SHIFT_FILE = "com/johnchourp/learnbyzantinemusic/music/BaseShift.kt"
        val OWN_DECLARATIONS = setOf("MIN_MORIA", "MAX_MORIA")
        val DECLARATION = Regex(
            """\b(BASE_SHIFT_MORIA_MIN|BASE_SHIFT_MORIA_MAX|BASE_SHIFT_MIN|BASE_SHIFT_MAX|MIN_MORIA|MAX_MORIA)\s*(?::\s*\w+\s*)?=(?!=)"""
        )
    }
}
