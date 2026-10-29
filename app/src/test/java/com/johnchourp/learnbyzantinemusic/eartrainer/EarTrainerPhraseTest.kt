package com.johnchourp.learnbyzantinemusic.eartrainer

import com.johnchourp.learnbyzantinemusic.modes.IsonDrone
import com.johnchourp.learnbyzantinemusic.modes.ModeLadders
import com.johnchourp.learnbyzantinemusic.music.Mode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * The question of «Μάντεψε τον ήχο» (ClickUp `869f4tqb9`): what it plays, where its pitches come
 * from, and that it can tell the eight ήχοι apart.
 */
class EarTrainerPhraseTest {

    @Test
    fun aroundTheBaseTheEightModesSitAtThesePositions() {
        // μόρια from the base of the ison, two φθόγγοι below to three above (audit 2026-10-29).
        val expected = mapOf(
            Mode.FIRST to listOf(-20, -12, 0, 10, 18, 30),
            Mode.SECOND to listOf(-22, -8, 0, 12, 20, 34),
            Mode.THIRD to listOf(-18, -6, 0, 12, 24, 30),
            Mode.FOURTH to listOf(-20, -12, 0, 12, 22, 30),
            Mode.PLAGAL_FIRST to listOf(-24, -12, 0, 10, 18, 30),
            Mode.PLAGAL_SECOND to listOf(-24, -4, 0, 6, 26, 30),
            Mode.VARYS to listOf(-18, -6, 0, 12, 24, 36),
            Mode.PLAGAL_FOURTH to listOf(-18, -8, 0, 12, 22, 30),
        )
        assertEquals(expected, Mode.entries.associateWith(EarTrainerPhrase::moriaFromBase))
    }

    @Test
    fun theEightModesAllSoundDifferent() {
        val shapes = Mode.entries.map(EarTrainerPhrase::moriaFromBase)
        assertEquals(Mode.entries.size, shapes.distinct().size)
    }

    @Test
    fun aboveTheBaseAloneTwoPairsWouldSoundTheSame() {
        // Why the phrase goes below the base too: drop those φθόγγοι and these pairs collide.
        fun above(mode: Mode) = EarTrainerPhrase.moriaFromBase(mode).drop(EarTrainerPhrase.STEPS_BELOW)
        assertEquals(above(Mode.FOURTH), above(Mode.PLAGAL_FOURTH))
        assertEquals(above(Mode.FIRST), above(Mode.PLAGAL_FIRST))
    }

    @Test
    fun theClosestPairIsThirdAndPlagalFourth() {
        // The FAQ names it: by the largest gap at any one φθόγγος, it is the only pair 2 μόρια apart.
        fun gap(a: Mode, b: Mode) = EarTrainerPhrase.moriaFromBase(a).zip(EarTrainerPhrase.moriaFromBase(b))
            .maxOf { (x, y) -> abs(x - y) }
        val pairs = Mode.entries.flatMapIndexed { i, a -> Mode.entries.drop(i + 1).map { b -> Triple(a, b, gap(a, b)) } }
        val closest = pairs.minOf { it.third }
        assertEquals(2, closest)
        assertEquals(listOf(Mode.THIRD to Mode.PLAGAL_FOURTH), pairs.filter { it.third == closest }.map { it.first to it.second })
    }

    @Test
    fun theClosestPairIsTellableByItsRegister() {
        // Γ΄ and Πλ.Δ΄ are 2 μόρια apart around the base; their bases are 30 μόρια apart. That is why
        // the phrase is played without any base shift.
        val third = EarTrainerPhrase.moriaFromBase(Mode.THIRD)
        val plagalFourth = EarTrainerPhrase.moriaFromBase(Mode.PLAGAL_FOURTH)
        assertEquals(2, third.zip(plagalFourth).maxOf { (a, b) -> abs(a - b) })
        val bases = EarTrainerPhrase.ison(Mode.THIRD).moriaFromNi - EarTrainerPhrase.ison(Mode.PLAGAL_FOURTH).moriaFromNi
        assertEquals(30, bases.value)
    }

    @Test
    fun everyQuestionStartsAndEndsOnTheBaseOfTheIson() {
        Mode.entries.forEach { mode ->
            val rungs = EarTrainerPhrase.rungs(mode)
            val ison = EarTrainerPhrase.ison(mode)
            assertEquals(IsonDrone.base(mode), ison.phthong)
            assertEquals("$mode starts on its base", ison, rungs.first())
            assertEquals("$mode ends on its base", ison, rungs.last())
        }
    }

    @Test
    fun theIsonIsThe8ModesPagesOwnWithoutAShift() {
        Mode.entries.forEach { mode ->
            val page = IsonDrone.held(IsonDrone.Request(mode, baseShiftMoria = 0))
            assertEquals("$mode", page, EarTrainerPhrase.ison(mode))
        }
    }

    @Test
    fun everyPitchIsARungOfTheModesOwnLadder() {
        Mode.entries.forEach { mode ->
            val ladder = ModeLadders.ladder(mode, baseShiftMoria = 0).steps
            EarTrainerPhrase.rungs(mode).forEach { rung ->
                assertTrue("$mode: ${rung.phthong.label} is on the ladder", rung in ladder)
            }
        }
    }

    @Test
    fun thePhraseMovesOneRungAtATime() {
        Mode.entries.forEach { mode ->
            val ascending = ModeLadders.ladder(mode, baseShiftMoria = 0).steps.reversed()
            val indices = EarTrainerPhrase.rungs(mode).map(ascending::indexOf)
            indices.zipWithNext().forEach { (a, b) -> assertEquals("$mode moves by one rung", 1, abs(a - b)) }
            assertEquals(EarTrainerPhrase.STEPS_ABOVE, indices.max() - indices.first())
            assertEquals(EarTrainerPhrase.STEPS_BELOW, indices.first() - indices.min())
        }
    }
}
