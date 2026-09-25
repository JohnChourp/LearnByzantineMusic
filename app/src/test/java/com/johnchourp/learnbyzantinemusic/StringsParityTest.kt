package com.johnchourp.learnbyzantinemusic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Every string exists in every language, with the same placeholders (ClickUp `869f5x24r`: "EL + EN
 * strings in parity", for the whole app rather than for one feature's keys).
 *
 * The app ships Greek in `values/` and English in `values-en/`. Neither kind of drift fails the build:
 * a key missing from `values-en/` silently shows Greek inside the English UI, and a placeholder that
 * differs — `%1$s` on one side, nothing or `%1$d` on the other — shows a wrong sentence, or throws,
 * in one language only, where nobody testing in the other one will see it.
 *
 * Measured when this test was written (2026-09-25): 1089 strings in each file, none missing either
 * way, placeholders identical. So the check is global from the start, and any `values-xx/` added
 * later is held to the same rule without editing this file.
 */
class StringsParityTest {

    private val resRoot: File by lazy {
        listOf(File("app/src/main/res"), File("src/main/res")).firstOrNull { it.isDirectory }
            ?: error("Cannot locate the resources from ${File("").absolutePath}")
    }

    /** Named entries of a folder's strings.xml, "tag:name" → text. Non-translatable ones are skipped. */
    private fun load(folder: File): Map<String, String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(folder, "strings.xml"))
        val entries = document.documentElement.childNodes
        return (0 until entries.length)
            .map { entries.item(it) }
            .filterIsInstance<Element>()
            .filter { it.hasAttribute("name") && it.getAttribute("translatable") != "false" }
            .associate { "${it.tagName}:${it.getAttribute("name")}" to it.textContent }
    }

    private val greek: Map<String, String> by lazy { load(File(resRoot, "values")) }

    /** Every other values-* folder that ships a strings.xml (values-night has colours only). */
    private val translations: Map<String, Map<String, String>> by lazy {
        resRoot.listFiles().orEmpty()
            .filter { it.isDirectory && it.name.startsWith("values-") && File(it, "strings.xml").isFile }
            .associate { it.name to load(it) }
    }

    private val placeholder = Regex("""%(\d+\$)?[-#+0,(]*\d*(\.\d+)?[sdfxXc]""")

    private fun placeholders(text: String): List<String> =
        placeholder.findAll(text.replace("%%", "")).map { it.value }.sorted().toList()

    @Test
    fun theStringsAreActuallyRead() {
        // Guards the slice: with nothing loaded, every comparison below would pass trivially.
        assertTrue("found ${greek.size} Greek strings", greek.size > 1000)
        assertTrue("found ${translations.keys}", "values-en" in translations)
        assertTrue("found ${translations["values-en"]?.size} English strings", translations.getValue("values-en").size > 1000)
    }

    @Test
    fun everyStringExistsInEveryLanguage() {
        translations.forEach { (folder, strings) ->
            assertEquals("in values/ but missing from $folder/", emptyList<String>(), (greek.keys - strings.keys).sorted())
            assertEquals("in $folder/ but missing from values/", emptyList<String>(), (strings.keys - greek.keys).sorted())
        }
    }

    @Test
    fun everyStringHasTheSamePlaceholdersInEveryLanguage() {
        translations.forEach { (folder, strings) ->
            val differing = greek.keys.intersect(strings.keys)
                .filter { key -> placeholders(greek.getValue(key)) != placeholders(strings.getValue(key)) }
                .sorted()
            assertEquals("placeholders differ between values/ and $folder/", emptyList<String>(), differing)
        }
    }

    @Test
    fun thePlaceholderPatternSeesWhatItMustSee() {
        // A pattern that matched nothing would make the test above pass on any drift.
        assertEquals(listOf("%1\$s", "%2\$d"), placeholders("Βήμα %2\$d: %1\$s"))
        assertEquals(listOf("%.1f"), placeholders("%.1f Hz, 100%% sure"))
        assertEquals(emptyList<String>(), placeholders("100% of the notes"))
    }
}
