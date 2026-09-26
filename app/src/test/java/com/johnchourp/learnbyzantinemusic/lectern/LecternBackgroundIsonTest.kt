package com.johnchourp.learnbyzantinemusic.lectern

import com.johnchourp.learnbyzantinemusic.lectern.LecternBackgroundIson.Heard
import com.johnchourp.learnbyzantinemusic.modes.IsonDrone
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.music.Phthong
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The reader and J5's background service in step (ClickUp `869f5x2e7`): the reader's own requests
 * coming back change nothing, a stop or a move from the notification is followed, and nothing is sent
 * twice.
 */
class LecternBackgroundIsonTest {

    private val pageOne = IsonDrone.Request(Mode.FIRST, 0)
    private val pageTwo = IsonDrone.Request(Mode.PLAGAL_FIRST, -3)
    private val pageThree = IsonDrone.Request(Mode.VARYS, 2, Phthong(PhthongName.DI))

    @Test
    fun `the reader's own requests coming back are echoes`() {
        val sync = LecternBackgroundIson()
        sync.start(null)
        assertTrue(sync.send(pageOne))
        assertEquals(Heard.Nothing, sync.onPublished(pageOne))
        assertTrue(sync.send(pageTwo))
        assertTrue(sync.send(pageThree))
        assertEquals("an earlier echo, overtaken", Heard.Nothing, sync.onPublished(pageTwo))
        assertEquals(Heard.Nothing, sync.onPublished(pageThree))
    }

    @Test
    fun `nothing is sent that the service is already going to play`() {
        val sync = LecternBackgroundIson()
        sync.start(null)
        assertFalse("silence to a silent service", sync.send(null))
        assertTrue(sync.send(pageOne))
        assertFalse("still on its way", sync.send(pageOne))
        sync.onPublished(pageOne)
        assertFalse("already playing", sync.send(pageOne))
        assertTrue(sync.send(pageTwo))
        assertTrue("back before it arrived: the service will be on page two", sync.send(pageOne))
    }

    @Test
    fun `a quick off and on is not undone by the stop coming back`() {
        val sync = LecternBackgroundIson()
        sync.start(null)
        sync.send(pageOne)
        sync.onPublished(pageOne)
        assertTrue(sync.send(null))
        assertTrue(sync.send(pageOne))
        // The service publishes the stop, then the start: neither is news.
        assertEquals(Heard.Nothing, sync.onPublished(null))
        assertEquals(Heard.Nothing, sync.onPublished(pageOne))
        // And a stop from the notification afterwards is still heard.
        assertEquals(Heard.Stopped, sync.onPublished(null))
    }

    @Test
    fun `a stop from outside is heard - the notification, the hour, the headphones`() {
        val sync = LecternBackgroundIson()
        sync.start(null)
        sync.send(pageTwo)
        sync.onPublished(pageTwo)
        assertEquals(Heard.Stopped, sync.onPublished(null))
        assertEquals("heard once", Heard.Nothing, sync.onPublished(null))
    }

    @Test
    fun `a φθόγγος moved from the notification is heard, and not sent back`() {
        val sync = LecternBackgroundIson()
        sync.start(null)
        sync.send(pageTwo)
        sync.onPublished(pageTwo)
        val moved = pageTwo.copy(choice = Phthong(PhthongName.DI))
        assertEquals(Heard.Moved(moved), sync.onPublished(moved))
        assertFalse("the reader now shows the move; sending it back would only echo", sync.send(moved))
    }

    @Test
    fun `while a request of the reader's is on its way, a change from outside is overtaken by it`() {
        val sync = LecternBackgroundIson()
        sync.start(null)
        sync.send(pageOne)
        sync.onPublished(pageOne)
        sync.send(pageTwo)
        // −/+ tapped on the old ison before page two's request arrived: the page turn lands after it.
        assertEquals(Heard.Nothing, sync.onPublished(pageOne.copy(choice = Phthong(PhthongName.DI))))
        assertEquals(Heard.Nothing, sync.onPublished(pageTwo))
    }

    @Test
    fun `a reader opened over a background ison shows it`() {
        val sync = LecternBackgroundIson()
        assertEquals(Heard.Moved(pageThree), sync.start(pageThree))
        assertFalse("and does not ask for it again", sync.send(pageThree))
        assertEquals(Heard.Nothing, LecternBackgroundIson().start(null))
    }

    @Test
    fun `after a refusal the reader plays it itself, and the service's silence is not a stop`() {
        val sync = LecternBackgroundIson()
        sync.start(null)
        sync.send(pageOne)
        sync.onRefused()
        assertEquals(Heard.Nothing, sync.onPublished(null))
        assertTrue("the next ison asks the service again", sync.send(pageTwo))
    }

    @Test
    fun `a page with a setting of its own takes over from an ison put there from outside, a page without keeps it`() {
        val leftPlaying = IsonDrone.Request(Mode.FOURTH, 0, Phthong(PhthongName.VOU))
        val setting = PageAssignment(3, Mode.PLAGAL_FIRST, -3)
        assertEquals("paging through a PDF with no settings", leftPlaying, LecternBackgroundIson.afterPageTurn(leftPlaying, holding = null))
        assertEquals(null, LecternBackgroundIson.afterPageTurn(leftPlaying, holding = setting))
        assertEquals(null, LecternBackgroundIson.afterPageTurn(null, holding = null))
    }

    @Test
    fun `requests that never come back cannot hold the queue`() {
        val sync = LecternBackgroundIson()
        sync.start(null)
        (0 until 40).forEach { shift -> sync.send(IsonDrone.Request(Mode.SECOND, shift % 13)) }
        sync.send(pageOne)
        assertEquals(Heard.Nothing, sync.onPublished(pageOne))
        assertEquals("the queue emptied with the last echo", Heard.Stopped, sync.onPublished(null))
    }
}
