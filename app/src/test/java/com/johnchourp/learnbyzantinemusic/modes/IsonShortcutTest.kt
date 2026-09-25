package com.johnchourp.learnbyzantinemusic.modes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The launcher shortcut «Ίσο» (ClickUp `869f5x2dq`).
 *
 * A static shortcut is resource XML that names a class and an extra by string, so nothing compiles
 * it against the code it opens: rename the activity or the extra and the shortcut quietly opens the
 * page without the ison — or not at all. This holds the XML to the code, and to what the shortcut is
 * for: it plays, it never records, and it is not a messaging shortcut.
 */
class IsonShortcutTest {

    private fun repoFile(path: String): File =
        listOf(File("app/$path"), File(path)).firstOrNull { it.exists() }
            ?: error("$path not found from ${File("").absolutePath}")

    private fun elements(parent: Element, tag: String): List<Element> {
        val nodes = parent.getElementsByTagName(tag)
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    private val shortcuts: List<Element> by lazy {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(repoFile("src/main/res/xml/shortcuts.xml"))
        elements(document.documentElement, "shortcut")
    }

    private val ison: Element by lazy {
        shortcuts.singleOrNull { it.getAttribute("android:shortcutId") == "ison" }
            ?: error("no shortcut with id «ison» in ${shortcuts.map { it.getAttribute("android:shortcutId") }}")
    }

    private val isonIntent: Element by lazy { elements(ison, "intent").single() }

    @Test
    fun bothShortcutsAreThereAndTheRecordingOneIsUntouched() {
        // Guards the slice: a parse that found nothing would let every assertion below pass.
        assertEquals(listOf("new_recording", "ison"), shortcuts.map { it.getAttribute("android:shortcutId") })
    }

    @Test
    fun itOpensTheEightModesPageAndNothingElse() {
        assertEquals("android.intent.action.VIEW", isonIntent.getAttribute("android:action"))
        assertEquals("com.johnchourp.learnbyzantinemusic", isonIntent.getAttribute("android:targetPackage"))
        assertEquals(
            "com.johnchourp.learnbyzantinemusic.modes.EightModesActivity",
            isonIntent.getAttribute("android:targetClass"),
        )
        // The class it names is real, and declared.
        assertTrue(repoFile("src/main/java/com/johnchourp/learnbyzantinemusic/modes/EightModesActivity.kt").isFile)
        assertTrue(repoFile("src/main/AndroidManifest.xml").readText().contains("android:name=\".modes.EightModesActivity\""))
    }

    @Test
    fun itAsksForTheIsonWithTheExtraTheActivityReads() {
        val extra = elements(isonIntent, "extra").singleOrNull()
        assertNotNull("the shortcut must carry the start-ison extra", extra)
        assertEquals(EightModesActivity.EXTRA_START_ISON, extra!!.getAttribute("android:name"))
        assertEquals("true", extra.getAttribute("android:value"))
    }

    @Test
    fun itIsNotAConversationShortcut() {
        val categories = elements(ison, "categories").map { it.getAttribute("android:name") }
        assertFalse("«Ίσο» is not a messaging shortcut: $categories", "android.shortcut.conversation" in categories)
    }

    @Test
    fun itsLabelsExistInBothLanguages() {
        listOf("android:shortcutShortLabel", "android:shortcutLongLabel").forEach { attribute ->
            val name = ison.getAttribute(attribute).removePrefix("@string/")
            listOf("src/main/res/values/strings.xml", "src/main/res/values-en/strings.xml").forEach { path ->
                assertTrue("$name missing from $path", repoFile(path).readText().contains("<string name=\"$name\">"))
            }
        }
    }
}
