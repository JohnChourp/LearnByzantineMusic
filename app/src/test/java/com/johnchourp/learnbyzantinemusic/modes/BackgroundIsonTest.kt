package com.johnchourp.learnbyzantinemusic.modes

import com.johnchourp.learnbyzantinemusic.modes.BackgroundIson.Sound
import com.johnchourp.learnbyzantinemusic.modes.ui.EIGHT_MODES
import com.johnchourp.learnbyzantinemusic.music.BaseShift
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.music.Phthong
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What the background ison decides (ClickUp `869f5x2dq`): the pitch it sounds, φθόγγος −/+ from its
 * notification, and when it stops by itself — the whole of `IsonPlaybackService`'s judgement, with
 * no device. The service only does what [Sound] says.
 */
class BackgroundIsonTest {

    private val hour = BackgroundIson.MAX_PLAY_MILLIS

    private fun request(mode: Mode = Mode.FIRST, shift: Int = 0, choice: Phthong? = null) =
        IsonDrone.Request(mode, shift, choice)

    // ---- the pitch ------------------------------------------------------------------------------

    @Test
    fun theBackgroundPitchIsThePagesForEveryModeChoiceAndShift() {
        var compared = 0
        EIGHT_MODES.forEach { row ->
            (BaseShift.MIN_MORIA..BaseShift.MAX_MORIA).forEach { shift ->
                // What the page sounds: its ladder, its choices, its lookup.
                val pageLadder = ModeLadders.ladder(row.scale, shift)
                val choices = IsonDrone.choices(row.mode, pageLadder)!!
                choices.all.forEach { choice ->
                    val background = BackgroundIson()
                    background.play(request(row.mode, shift, choice.takeIf { it != choices.base }), ToneTimbre.CLEAN, 0L)
                    val where = "${row.mode.key} shift=$shift ${choice.label}"
                    assertEquals(where, IsonDrone.step(pageLadder, choice)!!.frequencyHz, background.frequencyHz!!, 0.0)
                    assertEquals(where, choice, background.held)
                    compared++
                }
            }
        }
        // Guards the sweep: every mode × every shift × every φθόγγος, or it proves nothing.
        assertEquals(EIGHT_MODES.size * (BaseShift.MAX_MORIA - BaseShift.MIN_MORIA + 1) * PhthongName.entries.size, compared)
    }

    // ---- what a change means for the sound -----------------------------------------------------

    @Test
    fun fromSilenceItStartsAndTheSameRequestAgainChangesNothing() {
        val ison = BackgroundIson()
        assertNull(ison.frequencyHz)
        assertEquals(Sound.START, ison.play(request(), ToneTimbre.CLEAN, 0L))
        assertEquals(Sound.NONE, ison.play(request(), ToneTimbre.CLEAN, 1_000L))
    }

    @Test
    fun anotherPhthongOrShiftGlidesButAnotherTimbreStartsAfresh() {
        val ison = BackgroundIson()
        ison.play(request(), ToneTimbre.CLEAN, 0L)
        assertEquals(Sound.RETUNE, ison.play(request(choice = Phthong(PhthongName.DI)), ToneTimbre.CLEAN, 1L))
        assertEquals(Sound.RETUNE, ison.play(request(shift = 4, choice = Phthong(PhthongName.DI)), ToneTimbre.CLEAN, 2L))
        // A glide keeps the timbre it started with, so a new timbre is a new tone.
        assertEquals(Sound.START, ison.play(request(shift = 4, choice = Phthong(PhthongName.DI)), ToneTimbre.SOFT, 3L))
    }

    @Test
    fun stoppingTwiceIsStoppingOnce() {
        val ison = BackgroundIson()
        ison.play(request(), ToneTimbre.CLEAN, 0L)
        assertEquals(Sound.STOP, ison.stop())
        assertEquals(Sound.NONE, ison.stop())
        assertNull(ison.request)
        assertNull(ison.frequencyHz)
    }

    // ---- stopping by itself ----------------------------------------------------------------------

    @Test
    fun itStopsAnHourAfterItStartedAndMovingItDoesNotBuyMoreTime() {
        val ison = BackgroundIson()
        ison.play(request(), ToneTimbre.CLEAN, 1_000L)
        assertEquals(hour, ison.millisUntilTimeUp(1_000L))
        // Moving it — the page or the notification — does not restart the clock.
        ison.play(request(choice = Phthong(PhthongName.DI)), ToneTimbre.CLEAN, 1_000L + hour - 5_000L)
        ison.move(+1)
        assertFalse(ison.isTimeUp(1_000L + hour - 1L))
        assertTrue(ison.isTimeUp(1_000L + hour))
        assertEquals(0L, ison.millisUntilTimeUp(1_000L + hour + 60_000L))
    }

    @Test
    fun stoppingAndPlayingAgainStartsANewHour() {
        val ison = BackgroundIson()
        ison.play(request(), ToneTimbre.CLEAN, 0L)
        ison.stop()
        assertFalse("silent is never time up", ison.isTimeUp(2 * hour))
        assertNull(ison.millisUntilTimeUp(2 * hour))
        ison.play(request(), ToneTimbre.CLEAN, 2 * hour)
        assertFalse(ison.isTimeUp(3 * hour - 1L))
        assertTrue(ison.isTimeUp(3 * hour))
    }

    @Test
    fun unpluggedHeadphonesStopIt() {
        val ison = BackgroundIson()
        ison.play(request(), ToneTimbre.CLEAN, 0L)
        assertEquals(Sound.STOP, ison.onBecomingNoisy())
        assertNull(ison.request)
        assertEquals("already silent: nothing to stop", Sound.NONE, ison.onBecomingNoisy())
    }

    // ---- φθόγγος −/+ ---------------------------------------------------------------------------

    @Test
    fun minusAndPlusWalkTheChoicesByPitchAndStopAtTheEnds() {
        Mode.entries.forEach { mode ->
            val ladder = ModeLadders.ladder(mode, 0)
            val choices = IsonDrone.choices(mode, ladder)!!
            val byPitch = choices.all.sortedBy { ladder.stepFor(it)!!.moriaFromNi }

            val ison = BackgroundIson()
            ison.play(request(mode), ToneTimbre.CLEAN, 0L)
            while (ison.move(-1) == Sound.RETUNE) Unit
            assertEquals("${mode.key}: the bottom", byPitch.first(), ison.held)
            assertEquals("${mode.key}: nothing below the bottom", Sound.NONE, ison.move(-1))

            val walked = mutableListOf(ison.held!!)
            while (ison.move(+1) == Sound.RETUNE) walked += ison.held!!
            assertEquals("${mode.key}: every choice, low to high", byPitch, walked)
            assertEquals("${mode.key}: nothing above the top", Sound.NONE, ison.move(+1))
        }
    }

    @Test
    fun returningToTheBaseAsksForItAsThePageDoes() {
        // The base is «no choice», exactly as the page asks for it, so a request that comes back
        // to the base compares equal to the page's own.
        Mode.entries.forEach { mode ->
            val ison = BackgroundIson()
            ison.play(request(mode), ToneTimbre.CLEAN, 0L)
            // Off the base, whichever way there is room, and back.
            val away = if (ison.move(+1) == Sound.RETUNE) +1 else -1.also { ison.move(it) }
            assertNotNull("${mode.key}: moved off the base", ison.request!!.choice)
            ison.move(-away)
            assertEquals("${mode.key}", request(mode), ison.request)
        }
    }

    @Test
    fun aSilentIsonHasNothingToMove() {
        assertEquals(Sound.NONE, BackgroundIson().move(+1))
    }
}
