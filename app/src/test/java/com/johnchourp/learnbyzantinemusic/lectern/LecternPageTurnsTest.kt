package com.johnchourp.learnbyzantinemusic.lectern

import android.view.KeyEvent
import com.johnchourp.learnbyzantinemusic.lectern.LecternPageTurns.TapZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Every way the lectern's reader turns a page (ClickUp `869f5x2e7`): the volume keys, a Bluetooth
 * pedal in any of its modes, taps on the sides of the page, and swipes.
 */
class LecternPageTurnsTest {

    @Test
    fun `a pedal or a keyboard turns forward and back in every mode it can send`() {
        val forward = listOf(
            KeyEvent.KEYCODE_PAGE_DOWN,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_SPACE,
        )
        val back = listOf(
            KeyEvent.KEYCODE_PAGE_UP,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_UP,
        )
        forward.forEach { assertEquals("key $it", PageTurn.NEXT, LecternPageTurns.forKey(it, shiftPressed = false)) }
        back.forEach { assertEquals("key $it", PageTurn.PREVIOUS, LecternPageTurns.forKey(it, shiftPressed = false)) }
        assertEquals("Shift+space goes back, as in any reader", PageTurn.PREVIOUS, LecternPageTurns.forKey(KeyEvent.KEYCODE_SPACE, shiftPressed = true))
    }

    @Test
    fun `volume down turns forward and volume up back`() {
        assertEquals(PageTurn.NEXT, LecternPageTurns.forKey(KeyEvent.KEYCODE_VOLUME_DOWN, shiftPressed = false))
        assertEquals(PageTurn.PREVIOUS, LecternPageTurns.forKey(KeyEvent.KEYCODE_VOLUME_UP, shiftPressed = false))
    }

    @Test
    fun `every other key is left to the system`() {
        listOf(
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_TAB,
            KeyEvent.KEYCODE_VOLUME_MUTE,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_A,
            KeyEvent.KEYCODE_HOME,
        ).forEach { assertNull("key $it", LecternPageTurns.forKey(it, shiftPressed = false)) }
    }

    @Test
    fun `a press turns one page - never its release, never a held key's repeats`() {
        val key = KeyEvent.KEYCODE_PAGE_DOWN
        assertEquals(PageTurn.NEXT, LecternPageTurns.turnsOn(key, shiftPressed = false, isDown = true, repeatCount = 0))
        assertNull("the release", LecternPageTurns.turnsOn(key, shiftPressed = false, isDown = false, repeatCount = 0))
        (1..5).forEach { repeat ->
            assertNull("repeat $repeat", LecternPageTurns.turnsOn(key, shiftPressed = false, isDown = true, repeatCount = repeat))
        }
        assertNull("a key it does not read", LecternPageTurns.turnsOn(KeyEvent.KEYCODE_A, shiftPressed = false, isDown = true, repeatCount = 0))
    }

    @Test
    fun `the left third goes back, the right third forward, the middle shows the bars`() {
        val width = 900f
        assertEquals(TapZone.PREVIOUS, LecternPageTurns.forTap(0f, width))
        assertEquals(TapZone.PREVIOUS, LecternPageTurns.forTap(299f, width))
        assertEquals(TapZone.BARS, LecternPageTurns.forTap(300f, width))
        assertEquals(TapZone.BARS, LecternPageTurns.forTap(450f, width))
        assertEquals(TapZone.BARS, LecternPageTurns.forTap(600f, width))
        assertEquals(TapZone.NEXT, LecternPageTurns.forTap(601f, width))
        assertEquals(TapZone.NEXT, LecternPageTurns.forTap(900f, width))
        assertEquals("a page not laid out yet turns nothing", TapZone.BARS, LecternPageTurns.forTap(10f, 0f))
    }

    @Test
    fun `right to left is forward, and a short drag turns nothing`() {
        val threshold = 150f
        assertEquals(PageTurn.NEXT, LecternPageTurns.forSwipe(-150f, threshold))
        assertEquals(PageTurn.NEXT, LecternPageTurns.forSwipe(-600f, threshold))
        assertEquals(PageTurn.PREVIOUS, LecternPageTurns.forSwipe(150f, threshold))
        assertNull(LecternPageTurns.forSwipe(-149f, threshold))
        assertNull(LecternPageTurns.forSwipe(149f, threshold))
        assertNull(LecternPageTurns.forSwipe(0f, threshold))
    }

    @Test
    fun `a turn stops at the first and the last page`() {
        assertEquals(1, LecternPageTurns.target(0, PageTurn.NEXT, 10))
        assertEquals(0, LecternPageTurns.target(0, PageTurn.PREVIOUS, 10))
        assertEquals(9, LecternPageTurns.target(9, PageTurn.NEXT, 10))
        assertEquals(8, LecternPageTurns.target(9, PageTurn.PREVIOUS, 10))
        assertEquals(0, LecternPageTurns.target(0, PageTurn.NEXT, 1))
        assertEquals(0, LecternPageTurns.target(3, PageTurn.NEXT, 0))
    }
}
