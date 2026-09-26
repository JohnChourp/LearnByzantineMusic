package com.johnchourp.learnbyzantinemusic.lectern

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.FileNotFoundException
import java.io.IOException

/**
 * How the reader treats a PDF it cannot open, and how large it draws a page (ClickUp `869f5x2e7`): a
 * password or a damaged file is a message of its own, never a crash, and no page can ask for a bitmap
 * that would run the phone out of memory.
 */
class LecternPdfRulesTest {

    @Test
    fun `a password is told apart from a lost grant, though both arrive as SecurityException`() {
        // PdfRenderer throws SecurityException for a password; the provider throws it for a lost grant.
        assertEquals(LecternOpenFailure.PASSWORD_PROTECTED, LecternOpenFailure.whileParsing(SecurityException("password required")))
        assertEquals(LecternOpenFailure.NO_ACCESS, LecternOpenFailure.whileReading(SecurityException("no permission")))
    }

    @Test
    fun `every other failure has its own message`() {
        assertEquals(LecternOpenFailure.NOT_FOUND, LecternOpenFailure.whileReading(FileNotFoundException("gone")))
        assertEquals(LecternOpenFailure.UNREADABLE, LecternOpenFailure.whileReading(IOException("read error")))
        assertEquals(LecternOpenFailure.UNREADABLE, LecternOpenFailure.whileReading(IllegalStateException("provider died")))
        assertEquals(LecternOpenFailure.DAMAGED, LecternOpenFailure.whileParsing(IOException("cannot create document")))
        assertEquals(LecternOpenFailure.DAMAGED, LecternOpenFailure.whileParsing(IllegalStateException("native failure")))
    }

    @Test
    fun `a page is drawn as wide as it is shown, in its own proportions`() {
        // A4 is 595 × 842 points; a phone in portrait shows it 1080 px wide.
        assertEquals(1080 to 1528, LecternRenderSize.forPage(595, 842, 1080))
        // A landscape page is shorter than it is wide.
        assertEquals(1080 to 763, LecternRenderSize.forPage(842, 595, 1080))
    }

    @Test
    fun `no page asks for more than the cap, however it is shown`() {
        val cases = listOf(
            Triple(595, 842, 2960),   // A4 across a tablet in landscape
            Triple(2480, 3508, 2560), // a 300-dpi scan, in points as a scanner writes them
            Triple(100, 20_000, 1080), // a long strip
        )
        cases.forEach { (width, height, shownWidth) ->
            val (w, h) = LecternRenderSize.forPage(width, height, shownWidth)
            assertTrue("$width×$height at $shownWidth: ${w}×$h", w.toLong() * h <= LecternRenderSize.MAX_PIXELS)
            // Scaled down in both directions alike: the page keeps its shape.
            val aspect = height.toDouble() / width
            assertEquals("$width×$height aspect", aspect, h.toDouble() / w, aspect * 0.01)
        }
        assertTrue("16 MB of ARGB at most", LecternRenderSize.MAX_PIXELS * 4 <= 16_000_000L)
    }

    @Test
    fun `a page that reports no size, or a screen not laid out, still gets a drawable size`() {
        assertEquals(1080 to 1080, LecternRenderSize.forPage(0, 0, 1080))
        val (w, h) = LecternRenderSize.forPage(595, 842, 0)
        assertTrue(w >= 1 && h >= 1)
    }
}
