package com.johnchourp.learnbyzantinemusic.lessons.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ClickUp `869f5x2d7`: *«Λειτουργεί και όταν η συσκευή δεν έχει δονητή (απλώς κρύβεται η
 * επιλογή)»* — checked against a fake vibrator, with and without the hardware.
 */
class MetronomeVibrationOptionsTest {

    private class FakeVibrator(override val available: Boolean) : BeatVibrator {
        val pulses = mutableListOf<BeatStrength>()
        override fun pulse(strength: BeatStrength) {
            pulses += strength
        }
    }

    private fun play(beats: Int, count: Int, options: MetronomeOptions, vibrator: BeatVibrator): List<Boolean> {
        val clicks = mutableListOf<Boolean>()
        (0 until count).forEach { index ->
            MetronomeBeats.perform(MetronomeBeats.event(0L, 80, beats, index, options), { clicks += it }, vibrator)
        }
        return clicks
    }

    @Test
    fun withoutAVibratorTheOptionsAreHidden() {
        assertFalse(MetronomeBeats.showsVibrationOptions(FakeVibrator(available = false)))
        assertTrue(MetronomeBeats.showsVibrationOptions(FakeVibrator(available = true)))
    }

    @Test
    fun aMissingVibratorIsNeverAskedToPulseAndTheClickCarriesOn() {
        val none = FakeVibrator(available = false)
        val clicks = play(beats = 3, count = 6, options = MetronomeOptions(vibrate = true), vibrator = none)
        assertTrue(none.pulses.isEmpty())
        assertEquals(listOf(true, false, false, true, false, false), clicks)
    }

    @Test
    fun aStoredSilentCannotMuteAPhoneThatCannotVibrate() {
        // Restored from another phone, say: «Σιωπηλά» is on, but its switch is hidden here. It must
        // not leave a metronome that neither sounds nor vibrates.
        val effective = MetronomeOptions(vibrate = true, silent = true).effectiveOn(canVibrate = false)
        assertFalse(effective.vibrate)
        assertFalse(effective.silent)
        assertEquals(4, play(beats = 2, count = 4, options = effective, vibrator = FakeVibrator(available = false)).size)
    }

    @Test
    fun silentNeedsTheVibrationOn() {
        assertFalse(MetronomeOptions(vibrate = false, silent = true).effectiveOn(canVibrate = true).silent)
        assertTrue(MetronomeOptions(vibrate = true, silent = true).effectiveOn(canVibrate = true).silent)
    }

    @Test
    fun aVibratorFeelsExactlyWhatEachBeatSays() {
        val vibrator = FakeVibrator(available = true)
        play(beats = 3, count = 6, options = MetronomeOptions(vibrate = true), vibrator = vibrator)
        assertEquals(
            listOf(BeatStrength.STRONG, BeatStrength.LIGHT, BeatStrength.LIGHT, BeatStrength.STRONG, BeatStrength.LIGHT, BeatStrength.LIGHT),
            vibrator.pulses,
        )
    }

    @Test
    fun theDefaultsKeepTheVibrationTheMetronomeAlreadyHad() {
        // It vibrated on every beat before the switch existed (performHapticFeedback), and sounded.
        val defaults = MetronomeOptions()
        assertTrue(defaults.vibrate)
        assertFalse(defaults.silent)
        assertFalse(defaults.foot)
    }
}
