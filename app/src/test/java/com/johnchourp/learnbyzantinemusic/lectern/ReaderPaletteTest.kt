package com.johnchourp.learnbyzantinemusic.lectern

import com.johnchourp.learnbyzantinemusic.lectern.ui.readerPalette
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPalette
import org.junit.Assert.assertSame
import org.junit.Test

/** The page and the system bars of the lectern reader come from this one answer (ClickUp `869fhmxpt`). */
class ReaderPaletteTest {
    @Test
    fun nightTintOverALightThemeIsDark() {
        assertSame(LbmPalette.dark, readerPalette(LbmPalette.light, night = true))
    }

    @Test
    fun withoutTheTintTheAppPaletteStays() {
        assertSame(LbmPalette.light, readerPalette(LbmPalette.light, night = false))
    }

    @Test
    fun aDarkThemeIsNotReplacedByTheTint() {
        assertSame(LbmPalette.dark, readerPalette(LbmPalette.dark, night = true))
    }
}
