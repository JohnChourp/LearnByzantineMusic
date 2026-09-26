package com.johnchourp.learnbyzantinemusic.lectern

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The reader's night tint (ClickUp `869f5x2e7`): paper goes dark and ink light — but the red of the
 * martyries and φθορές stays red, which a plain inversion would turn cyan.
 */
class LecternNightTintTest {

    @Test
    fun `it is a 4 by 5 colour matrix that leaves alpha alone`() {
        val matrix = LecternNightTint.MATRIX
        assertEquals(20, matrix.size)
        assertEquals(listOf(0f, 0f, 0f, 1f, 0f), matrix.slice(15..19))
        (0 until 3).forEach { row -> assertEquals("row $row takes no alpha", 0f, matrix[row * 5 + 3]) }
    }

    @Test
    fun `white paper goes black`() {
        assertEquals(Triple(0, 0, 0), LecternNightTint.apply(255, 255, 255))
    }

    @Test
    fun `black ink goes light, a little dimmed and warm rather than full white`() {
        val (r, g, b) = LecternNightTint.apply(0, 0, 0)
        assertTrue("light: $r $g $b", minOf(r, g, b) >= 190)
        assertTrue("not full white", maxOf(r, g, b) < 255)
        assertTrue("warm: red ≥ green ≥ blue", r >= g && g >= b)
    }

    @Test
    fun `red ink stays red, where a plain inversion would make it cyan`() {
        val (r, g, b) = LecternNightTint.apply(200, 30, 30)
        assertTrue("red still leads: $r $g $b", r > g + 60 && r > b + 60)
        // The inversion this replaces: 255 − c.
        val (ir, ig, ib) = Triple(255 - 200, 255 - 30, 255 - 30)
        assertTrue("a plain inversion is cyan", ig > ir && ib > ir)
    }

    @Test
    fun `lighter tones end darker and darker ones lighter, in order`() {
        val greys = (0..255 step 15).map { LecternNightTint.apply(it, it, it).first }
        assertEquals(greys.sortedDescending(), greys)
    }
}
