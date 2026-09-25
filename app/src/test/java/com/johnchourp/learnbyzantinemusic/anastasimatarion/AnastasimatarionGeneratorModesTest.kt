package com.johnchourp.learnbyzantinemusic.anastasimatarion

import com.johnchourp.learnbyzantinemusic.music.Mode
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * `scripts/generate-anastasimatarion-catalog.mjs` writes the catalog asset, and keeps its own list of
 * the eight modes. CI runs no Node tests, so this JVM test reads the script and holds that list to
 * [Mode]: the same keys, in the same order, with `ToneNSun.html` numbered as [Mode.number]
 * (ClickUp `869f5x299`). `AnastasimatarionCatalogTest` checks the generated asset the same way.
 */
class AnastasimatarionGeneratorModesTest {

    private val script: String by lazy {
        val name = "scripts/generate-anastasimatarion-catalog.mjs"
        val file = listOf(File(name), File("../$name")).firstOrNull { it.isFile }
            ?: error("Cannot locate $name from ${File("").absolutePath}")
        file.readText(Charsets.UTF_8)
    }

    /** The script's `MODES` entries: each key, and the N of its `ToneNSun.html` page. */
    private val entries: List<Pair<String, Int>> by lazy {
        Regex("""\{\s*key:\s*'([a-z_]+)',\s*page:\s*'Tone(\d+)Sun\.html'""")
            .findAll(script)
            .map { match -> match.groupValues[1] to match.groupValues[2].toInt() }
            .toList()
    }

    @Test
    fun theGeneratorListsTheModesOfTheApp() {
        // Guards the pattern: a script that changed shape must fail here, not pass on zero entries.
        assertEquals("entries found: $entries", 8, entries.size)
        assertEquals(Mode.entries.map { it.key }, entries.map { it.first })
        assertEquals(Mode.entries.map { it.number }, entries.map { it.second })
    }
}
