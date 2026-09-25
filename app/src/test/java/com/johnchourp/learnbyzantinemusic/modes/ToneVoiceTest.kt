package com.johnchourp.learnbyzantinemusic.modes

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs

/**
 * Moving the ison must not click (ClickUp `869f5x251`).
 *
 * A click is a jump between two neighbouring samples that the tone itself could never make. A sine
 * can only move by its slope from one sample to the next, so a retune that keeps the stream going is
 * held to that slope — across the very sample where the pitch starts to move. Stopping and starting
 * the tone instead, which is what moving the drone used to do, jumps; the negative control below
 * shows the measure catches exactly that.
 */
class ToneVoiceTest {

    /** The player's own level. */
    private val amplitude = Short.MAX_VALUE * 0.18
    private val sampleRate = 44_100.0

    private fun render(voice: ToneVoice, samples: Int): ShortArray =
        ShortArray(samples).also { voice.render(it, amplitude) }

    /** The largest step between neighbouring samples after the attack — where a click would show. */
    private fun largestStep(signal: ShortArray): Int =
        (ToneVoice.ATTACK_SAMPLES + 1 until signal.size).maxOf { abs(signal[it] - signal[it - 1]) }

    /** The steepest step a pure sine at [hz] can take, plus rounding to 16 bits. */
    private fun sineLimit(hz: Double): Double = amplitude * 2 * PI * hz / sampleRate + 2

    @Test
    fun aRetuneGlidesInsideTheSameStreamWithoutAJump() {
        val voice = ToneVoice(220.0, ToneTimbre.CLEAN)
        val signal = render(voice, 4_096) + run {
            voice.retune(330.0)
            render(voice, 8_192)
        }
        assertEquals("the retune must actually have happened", 330.0, voice.frequencyHz, 0.0)
        val steepest = largestStep(signal)
        assertTrue("a step of $steepest across the retune", steepest <= sineLimit(330.0))
    }

    @Test
    fun noTimbreJumpsAcrossARetune() {
        ToneTimbre.entries.forEach { timbre ->
            val voice = ToneVoice(196.0, timbre)
            val signal = render(voice, 4_096) + run {
                voice.retune(293.66)
                render(voice, 8_192)
            }
            // Harmonics make a timbre up to about 2.5 times steeper than a pure sine; a click is
            // several times more than even that.
            val steepest = largestStep(signal)
            assertTrue("$timbre: a step of $steepest", steepest <= 3 * sineLimit(293.66))
        }
    }

    @Test
    fun restartingInsteadWouldJump() {
        // Negative control: without it, the limits above could pass by measuring nothing.
        val old = render(ToneVoice(220.0, ToneTimbre.CLEAN), 4_096)
        val cut = (ToneVoice.ATTACK_SAMPLES until old.size).last { abs(old[it].toInt()) > 0.9 * amplitude }
        val restarted = old.copyOf(cut + 1) + render(ToneVoice(330.0, ToneTimbre.CLEAN), 4_096)
        val steepest = largestStep(restarted)
        assertTrue("a restart stepped only $steepest", steepest > 10 * sineLimit(330.0))
    }

    @Test
    fun theGlideTakes50msAndLandsExactlyOnTheNewPitch() {
        val voice = ToneVoice(220.0, ToneTimbre.CLEAN)
        render(voice, 1_000)
        voice.retune(330.0)

        val half = ToneVoice.GLIDE_SAMPLES / 2
        render(voice, half)
        val halfway = voice.frequencyHz
        assertTrue("halfway through the glide the pitch is $halfway", halfway > 220.0 && halfway < 330.0)

        render(voice, ToneVoice.GLIDE_SAMPLES - half - 1)
        assertTrue("one sample early it has not landed", voice.frequencyHz < 330.0)
        render(voice, 1)
        // Exactly, not nearly: the drone has to end on the ladder's own frequency.
        assertEquals(330.0, voice.frequencyHz, 0.0)
        render(voice, 5_000)
        assertEquals(330.0, voice.frequencyHz, 0.0)
    }

    @Test
    fun aRetuneMidGlideCarriesOnFromWhereThePitchIs() {
        val voice = ToneVoice(220.0, ToneTimbre.CLEAN)
        val start = render(voice, 2_000)
        voice.retune(330.0)
        val towardsFirst = render(voice, ToneVoice.GLIDE_SAMPLES / 3)
        val wherePitchWas = voice.frequencyHz
        voice.retune(196.0)
        val firstStep = render(voice, 1)
        val restartedFrom = voice.frequencyHz
        assertTrue("the second glide started from $restartedFrom", abs(restartedFrom - wherePitchWas) < 1.0)
        val rest = render(voice, 6_000)
        assertEquals(196.0, voice.frequencyHz, 0.0)
        val steepest = largestStep(start + towardsFirst + firstStep + rest)
        assertTrue("a step of $steepest", steepest <= sineLimit(330.0))
    }

    @Test
    fun withoutAMoveTheToneIsUntouched() {
        // A retune to the pitch already sounding — the screen re-sending the same frequency — must
        // not start a glide, or anything else audible.
        val steady = ToneVoice(246.94, ToneTimbre.SOFT)
        val resent = ToneVoice(246.94, ToneTimbre.SOFT)
        val expected = render(steady, 3_000)
        resent.retune(246.94)
        assertArrayEquals(expected, render(resent, 3_000))
        assertEquals(246.94, steady.frequencyHz, 0.0)
    }

    @Test
    fun aSilentPlayerHasNothingToRetune() {
        // False tells the caller to start a tone instead; nothing here touches an AudioTrack.
        val player = PhthongTonePlayer()
        assertFalse(player.retune(220.0))
        assertFalse(player.retune(Double.NaN))
        assertFalse(player.retune(-1.0))
        assertFalse(player.retune(Double.POSITIVE_INFINITY))
    }
}
