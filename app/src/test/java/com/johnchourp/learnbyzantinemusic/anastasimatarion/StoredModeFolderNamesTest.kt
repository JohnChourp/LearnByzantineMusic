package com.johnchourp.learnbyzantinemusic.anastasimatarion

import com.johnchourp.learnbyzantinemusic.music.Mode
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The folder of each mode under «Αναστασιματάριο» is on users' devices: a renamed folder would hide
 * every recording already in it. Since ClickUp `869f5x299` the names live in [Mode]; they must still
 * be, byte for byte, the ones the app has always written.
 *
 * The expected names are spelled as UTF-8 bytes on purpose. Seven of them end in «΄», U+0384 GREEK
 * TONOS (UTF-8 `ce 84`), which an editor, a font or a "tidy-up" can turn into an apostrophe or a combining
 * accent without anyone seeing the difference — only the bytes show it.
 */
class StoredModeFolderNamesTest {

    private val storedBytes = mapOf(
        Mode.FIRST to "ce89cf87cebfcf8220ce91ce84", // Ήχος Α΄
        Mode.SECOND to "ce89cf87cebfcf8220ce92ce84", // Ήχος Β΄
        Mode.THIRD to "ce89cf87cebfcf8220ce93ce84", // Ήχος Γ΄
        Mode.FOURTH to "ce89cf87cebfcf8220ce94ce84", // Ήχος Δ΄
        Mode.PLAGAL_FIRST to "ce89cf87cebfcf8220cf80cebb2e20ce91ce84", // Ήχος πλ. Α΄
        Mode.PLAGAL_SECOND to "ce89cf87cebfcf8220cf80cebb2e20ce92ce84", // Ήχος πλ. Β΄
        Mode.VARYS to "ce89cf87cebfcf8220ce92ceb1cf81cf8dcf82", // Ήχος Βαρύς
        Mode.PLAGAL_FOURTH to "ce89cf87cebfcf8220cf80cebb2e20ce94ce84", // Ήχος πλ. Δ΄
    )

    @Test
    fun everyModeFolderIsByteForByteTheStoredName() {
        assertEquals("all eight modes", Mode.entries.toSet(), storedBytes.keys)
        Mode.entries.forEach { mode ->
            assertEquals(mode.key, storedBytes.getValue(mode), utf8Hex(mode.anastasimatarionFolder))
            // And that is the name the recordings code actually builds its paths from.
            assertEquals(mode.key, storedBytes.getValue(mode), utf8Hex(HymnFolders.modeFolder(mode.key)))
        }
    }

    @Test
    fun theTonosIsTheGreekOneNotALookalike() {
        val tonos = '΄'
        Mode.entries.filter { it != Mode.VARYS }.forEach { mode ->
            assertEquals(mode.key, tonos, mode.anastasimatarionFolder.last())
        }
    }

    private fun utf8Hex(text: String): String =
        text.toByteArray(Charsets.UTF_8).joinToString("") { "%02x".format(it) }
}
