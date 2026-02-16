package com.johnchourp.learnbyzantinemusic.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardIniParserTest {
    @Test
    fun `parses unique tokens from keyboard ini sections`() {
        val content = javaClass.classLoader
            ?.getResourceAsStream("keyboard_sample.ini")
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: error("Missing test resource keyboard_sample.ini")

        val entries = KeyboardIniParser().parse(content)

        assertEquals(11, entries.size)
        assertEquals("a1", entries.first().token)
        assertEquals(97, entries.first().keycode)
        assertTrue(entries.any { it.token == "xr1" && it.layer == 5 })
    }
}
