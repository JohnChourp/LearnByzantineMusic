package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.modes.IsonDrone
import com.johnchourp.learnbyzantinemusic.music.BaseShift
import com.johnchourp.learnbyzantinemusic.music.ByzantineTuning
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.music.Phthong
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The ison under «Ψάλλε μαζί» (ClickUp `869f5x2cv`, J2). With a ήχος chosen it holds that ήχος's base
 * — the end of its απήχημα (F3) — at exactly the pitch the 8 Ήχοι page's ison sounds, «Μεταφορά βάσης»
 * included: the same lookup on the same ladder, never a second calculation. «Διατονικός» is no ήχος,
 * so it holds Νη, the Trainer's own base: 220 Hz.
 */
class SingAlongIsonTest {

    private val shifts = listOf(BaseShift.MIN_MORIA, -6, -1, 0, 5, BaseShift.MAX_MORIA)

    @Test
    fun inEveryModeItIsThe8ModesPagesIsonToTheHertz() {
        Mode.entries.forEach { mode ->
            shifts.forEach { shift ->
                val scale = TrainerScale(mode, shift)
                val page = IsonDrone.held(IsonDrone.Request(mode, shift))!!
                assertEquals("$mode at $shift", page.frequencyHz, SingAlong.isonFrequencyHz(scale)!!, 0.0)
                assertEquals(IsonDrone.base(mode), SingAlong.isonPhthong(scale))
            }
        }
    }

    @Test
    fun itSitsOnTheLadderTheMelodyPlaysOn() {
        // The guide's notes come from the Trainer's ladder; the ison from the page's. Where they
        // overlap they are one ladder, so the ison is in tune with the guide.
        Mode.entries.forEach { mode ->
            shifts.forEach { shift ->
                val scale = TrainerScale(mode, shift)
                val onTrainersLadder = scale.ladder.stepFor(IsonDrone.base(mode))!!.frequencyHz
                assertEquals("$mode at $shift", onTrainersLadder, SingAlong.isonFrequencyHz(scale)!!, 1e-9)
            }
        }
    }

    @Test
    fun itFollowsTheBaseShiftByExactlyThatMany() {
        Mode.entries.forEach { mode ->
            val atZero = SingAlong.isonFrequencyHz(TrainerScale(mode, 0))!!
            val lower = SingAlong.isonFrequencyHz(TrainerScale(mode, -6))!!
            assertEquals("$mode", ByzantineTuning.ratioForMoria(-6.0), lower / atZero, 1e-12)
        }
    }

    @Test
    fun theDiatonicDefaultHoldsNiAt220Hz() {
        assertEquals(Phthong(PhthongName.NI), SingAlong.isonPhthong(TrainerScale.DIATONIC))
        assertEquals(ByzantineTuning.NI_BASE_HZ, SingAlong.isonFrequencyHz(TrainerScale.DIATONIC)!!, 1e-9)
    }
}
