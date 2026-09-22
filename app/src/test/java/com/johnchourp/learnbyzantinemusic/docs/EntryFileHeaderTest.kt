package com.johnchourp.learnbyzantinemusic.docs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * ClickUp `869f4tpyp` (C3): documentation lives in the code, so every entry file answers "what do I
 * do and what do I store" without opening anything else.
 *
 * This keeps that true for screens written later. It checks the *position* of the block — a KDoc
 * somewhere in the middle of the file documents a helper, not the screen — and a minimum length,
 * because `/** The settings screen. */` satisfies a naive check while telling the reader nothing.
 */
class EntryFileHeaderTest {

    private val minimumHeaderChars = 200

    private val activities: List<File> by lazy {
        val root = listOf(File("app/src/main/java"), File("src/main/java")).firstOrNull { it.isDirectory }
            ?: error("Cannot locate the main source root from ${File("").absolutePath}")
        root.walkTopDown().filter { it.isFile && it.name.endsWith("Activity.kt") }.toList()
    }

    @Test
    fun theEntryFilesAreActuallyFound() {
        // Guards the slice: an empty list would make every assertion below trivially true.
        assertTrue("expected to find the app's activities, found ${activities.size}", activities.size >= 12)
    }

    @Test
    fun everyEntryFileOpensWithADocCommentAboveItsClass() {
        val undocumented = activities.filter { file ->
            val text = file.readText()
            val classAt = Regex("""(?m)^(internal |abstract |open )*class \w+""").find(text)?.range?.first
                ?: error("${file.name} declares no class")
            val header = Regex("""(?s)/\*\*.*?\*/""").findAll(text.substring(0, classAt)).lastOrNull()?.value
            header == null || header.length < minimumHeaderChars
        }.map { it.name }.sorted()

        assertEquals(
            "these entry files have no usable header above their class declaration",
            emptyList<String>(),
            undocumented
        )
    }
}
