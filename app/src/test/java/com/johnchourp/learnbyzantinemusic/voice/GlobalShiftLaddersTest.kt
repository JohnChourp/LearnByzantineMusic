package com.johnchourp.learnbyzantinemusic.voice

import com.johnchourp.learnbyzantinemusic.docs.KotlinSource
import com.johnchourp.learnbyzantinemusic.modes.IsonDrone
import com.johnchourp.learnbyzantinemusic.modes.ModeLadders
import com.johnchourp.learnbyzantinemusic.music.BaseShift
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.music.ModeLadder
import com.johnchourp.learnbyzantinemusic.music.Moria
import com.johnchourp.learnbyzantinemusic.trainer.TrainerScale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The voice's global shift (ClickUp `869f5x2dd`, J4) on every ladder the app builds: the 8 Ήχοι
 * diagram — and with it the απήχημα and «Πού είμαι» — the ison, the page's and the background
 * service's alike (both through [ModeLadders]), and the Melody Trainer, «Διατονικός» included.
 *
 * A global 0 is the app exactly as it was, bit for bit. Any other value moves every ladder by
 * exactly that many μόρια on top of the mode's own shift, and the sum is clamped where it is used.
 */
class GlobalShiftLaddersTest {

    private val globals = listOf(BaseShift.MIN_MORIA, -20, -1, 1, 20, BaseShift.MAX_MORIA)

    /** The Trainer's ladder as it was built before J4: the mode's own shift, and nothing else. */
    private fun trainerLadderBeforeJ4(scale: TrainerScale): ModeLadder =
        scale.definition.ladder(
            octaves = TrainerScale.OCTAVES,
            baseShift = Moria(scale.baseShiftMoria),
            lowestOctave = TrainerScale.LOWEST_OCTAVE,
        )

    // ---- a global 0 changes nothing ---------------------------------------------------------------

    @Test
    fun aGlobalZeroLeavesTheEightModesLadderAndItsIsonBitForBit() {
        var compared = 0
        Mode.entries.forEach { mode ->
            BaseShift.RANGE.forEach { own ->
                val where = "${mode.key} own=$own"
                val shift = BaseShift.combined(own, BaseShift.DEFAULT_MORIA)
                assertEquals(where, own, shift)
                // exact List<Double> equality: a tolerance would hide a second calculation
                assertEquals(where, ModeLadders.ladder(mode, own).frequencies, ModeLadders.ladder(mode, shift).frequencies)
                val ladder = ModeLadders.ladder(mode, own)
                IsonDrone.choices(mode, ladder)!!.all.forEach { choice ->
                    assertEquals(
                        "$where ${choice.label}",
                        IsonDrone.held(IsonDrone.Request(mode, own, choice))!!.frequencyHz,
                        IsonDrone.held(IsonDrone.Request(mode, shift, choice))!!.frequencyHz,
                        0.0,
                    )
                }
                compared++
            }
        }
        assertEquals(Mode.entries.size * BaseShift.RANGE.count(), compared)
    }

    @Test
    fun aGlobalZeroLeavesTheTrainerBitForBit() {
        var compared = 0
        (Mode.entries + listOf<Mode?>(null)).forEach { mode ->
            val owns = if (mode == null) listOf(BaseShift.DEFAULT_MORIA) else BaseShift.RANGE.toList()
            owns.forEach { own ->
                val scale = TrainerScale(mode, own, BaseShift.DEFAULT_MORIA)
                assertEquals("${mode?.key} own=$own", trainerLadderBeforeJ4(scale).frequencies, scale.ladder.frequencies)
                compared++
            }
        }
        assertEquals(Mode.entries.size * BaseShift.RANGE.count() + 1, compared)
        // The default the Trainer opens on is still «Διατονικός» at Νη = 220 Hz.
        assertEquals(TrainerScale(), TrainerScale.DIATONIC)
        assertEquals(BaseShift.DEFAULT_MORIA, TrainerScale.DIATONIC.ladderShiftMoria)
    }

    // ---- any other global moves every ladder by exactly that much ----------------------------------

    @Test
    fun aGlobalShiftMovesEveryEightModesLadderAndTheIsonByExactlyThatMuch() {
        Mode.entries.forEach { mode ->
            val unshifted = ModeLadders.ladder(mode, BaseShift.DEFAULT_MORIA)
            val base = IsonDrone.held(IsonDrone.Request(mode, BaseShift.DEFAULT_MORIA))!!
            globals.forEach { global ->
                val where = "${mode.key} global=$global"
                val shift = BaseShift.combined(BaseShift.DEFAULT_MORIA, global)
                val shifted = ModeLadders.ladder(mode, shift)
                unshifted.steps.indices.forEach { i ->
                    assertEquals("$where rung $i", unshifted.steps[i].moriaFromNi + Moria(global), shifted.steps[i].moriaFromNi)
                }
                val held = IsonDrone.held(IsonDrone.Request(mode, shift))!!
                assertEquals(where, base.phthong, held.phthong)
                assertEquals(where, base.moriaFromNi + Moria(global), held.moriaFromNi)
            }
        }
    }

    @Test
    fun aGlobalShiftMovesTheTrainerDiatonicIncludedByExactlyThatMuch() {
        (Mode.entries + listOf<Mode?>(null)).forEach { mode ->
            val unshifted = TrainerScale(mode).ladder
            globals.forEach { global ->
                val shifted = TrainerScale(mode, BaseShift.DEFAULT_MORIA, global).ladder
                unshifted.steps.indices.forEach { i ->
                    assertEquals(
                        "${mode?.key ?: "Διατονικός"} global=$global rung $i",
                        unshifted.steps[i].moriaFromNi + Moria(global),
                        shifted.steps[i].moriaFromNi,
                    )
                }
            }
        }
    }

    @Test
    fun theGlobalShiftAddsToTheModesOwnAndTheSumIsClamped() {
        // Added: own 12 and global 7 sound where own 19 alone would.
        assertEquals(ModeLadders.ladder(Mode.FIRST, 19).frequencies, ModeLadders.ladder(Mode.FIRST, BaseShift.combined(12, 7)).frequencies)
        assertEquals(19, TrainerScale(Mode.FIRST, 12, 7).ladderShiftMoria)
        // Clamped, not wrapped: own 30 and global 20 sound at the top of the range, not at +50.
        assertEquals(
            ModeLadders.ladder(Mode.FIRST, BaseShift.MAX_MORIA).frequencies,
            ModeLadders.ladder(Mode.FIRST, BaseShift.combined(30, 20)).frequencies,
        )
        assertEquals(BaseShift.MAX_MORIA, TrainerScale(Mode.FIRST, 30, 20).ladderShiftMoria)
        assertEquals(BaseShift.MIN_MORIA, TrainerScale(Mode.FIRST, -30, -20).ladderShiftMoria)
        // Neither stored value is touched: the scale still holds both as they were given.
        val both = TrainerScale(Mode.FIRST, 30, 20)
        assertEquals(30, both.baseShiftMoria)
        assertEquals(20, both.globalShiftMoria)
    }

    // ---- the wiring: every ladder takes the combined shift ------------------------------------------

    /**
     * On the 8 Ήχοι page every ladder — diagram, απήχημα, «Πού είμαι», ison — is built from
     * `ladderShift(…)`, the combined shift; the mode's own value reaches only its slider.
     */
    @Test
    fun everyEightModesLadderIsBuiltWithTheCombinedShift() {
        val main = KotlinSource.mainRoot
        val screen = KotlinSource.withoutComments(
            File(main, "com/johnchourp/learnbyzantinemusic/modes/ui/EightModesScreen.kt").readText(),
        )
        assertTrue(
            "ladderShift must combine the two",
            "BaseShift.combined(baseShiftByMode[modeIndex] ?: BaseShift.DEFAULT_MORIA, globalShiftMoria)" in screen,
        )
        assertTrue("val shift = ladderShift(selectedModeIndex)" in screen)
        assertTrue("IsonDrone.Request(EIGHT_MODES[selectedModeIndex].mode, shift, isonChoice)" in screen)
        val arguments = Regex("""\bbaseShiftMoria\s*=\s*([^,\n]+)""").findAll(screen).map { it.groupValues[1].trim() }.toList()
        // The diagram, «Πού είμαι», the details card, and the απήχημα player the details card forwards to.
        assertEquals(arguments.toString(), 4, arguments.size)
        arguments.forEach { value ->
            assertTrue("a ladder built from $value", value.startsWith("ladderShift(") || value == "baseShiftMoria")
        }
        // The mode's own value is read in two places only: the combination, and its own slider.
        val ownReads = Regex("""baseShiftByMode\[[^\]]+]\s*\?:""").findAll(screen).count()
        assertEquals(2, ownReads)
        assertTrue("moria = baseShiftByMode[selectedModeIndex] ?: 0" in screen)

        val activity = KotlinSource.withoutComments(
            File(main, "com/johnchourp/learnbyzantinemusic/modes/EightModesActivity.kt").readText(),
        )
        assertTrue("globalShiftMoria = globalShiftMoria" in activity)
        // Read on creation and on every return: Settings may change it while the page waits.
        assertEquals(2, Regex("""globalShiftMoria = GlobalShift\.load\(this\)""").findAll(activity).count())
    }

    /**
     * The Trainer takes the global shift on opening and on every return, and keeps it when the ήχος
     * changes or a saved melody opens — a melody carries its ήχος and shift, never the singer's voice.
     */
    @Test
    fun theTrainerKeepsTheGlobalShiftWhenItsScaleChanges() {
        val trainer = KotlinSource.withoutComments(
            File(KotlinSource.mainRoot, "com/johnchourp/learnbyzantinemusic/trainer/MelodyTrainerActivity.kt").readText(),
        )
        listOf(
            "scale = scale.copy(globalShiftMoria = GlobalShift.load(this))",
            "scale = TrainerScale(mode, shift, scale.globalShiftMoria)",
            "globalShiftMoria = scale.globalShiftMoria",
            "scale = live.scale.copy(globalShiftMoria = scale.globalShiftMoria)",
        ).forEach { wiring -> assertTrue("MelodyTrainerActivity: $wiring", wiring in trainer) }
        val scale = KotlinSource.withoutComments(
            File(KotlinSource.mainRoot, "com/johnchourp/learnbyzantinemusic/trainer/TrainerScale.kt").readText(),
        )
        assertTrue("the ladder uses the combined shift", "baseShift = Moria(ladderShiftMoria)" in scale)
    }
}
