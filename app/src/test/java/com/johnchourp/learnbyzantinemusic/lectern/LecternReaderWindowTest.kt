package com.johnchourp.learnbyzantinemusic.lectern

import com.johnchourp.learnbyzantinemusic.docs.KotlinSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * What the lectern's reader does with its window (ClickUp `869f5x2e7`) — the lines no JVM test can run,
 * so they are checked in the source, comments removed ([KotlinSource]), each check reading only the
 * function it is about:
 *
 * - it is the **only** screen that turns to landscape; every other one keeps BaseActivity's portrait;
 * - it keeps the screen on, and takes the page-turn keys before the system;
 * - onStop tells the ViewModel whether this is only a rotation, so a rotation silences nothing and
 *   leaving stops the ison (J5's rule);
 * - both screens are private, and reading the user's own PDFs needs **no** storage permission.
 */
class LecternReaderWindowTest {

    private val androidNs = "http://schemas.android.com/apk/res/android"

    private val sources: List<File> by lazy {
        KotlinSource.mainRoot.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    private fun code(path: String): String =
        KotlinSource.withoutComments(File(KotlinSource.mainRoot, "com/johnchourp/learnbyzantinemusic/$path").readText())

    private val reader by lazy { code("lectern/LecternReaderActivity.kt") }

    /** From the declaration to the next class-level member; null when the function does not exist. */
    private fun bodyOf(text: String, declaration: String): String? {
        val start = text.indexOf(declaration)
        if (start < 0) return null
        val rest = text.substring(start)
        val next = Regex("""\n {4}(private |override |protected |internal )*(fun|val|var|companion|class|enum|object) """)
            .find(rest, startIndex = 1)
        return if (next != null) rest.substring(0, next.range.first) else rest
    }

    private val orientationOverride = Regex("""\boverride\s+val\s+screenOrientation\b""")
    private val orientationWrite = Regex("""\brequestedOrientation\s*=""")

    @Test
    fun theSourcesAreActuallyRead() {
        // Guards the slice: every check below would pass over nothing.
        assertTrue("expected the app's sources, found ${sources.size}", sources.size > 100)
        assertTrue(reader.contains("class LecternReaderActivity"))
    }

    @Test
    fun onlyTheReaderTurnsToLandscape() {
        val overriding = sources.filter { orientationOverride.containsMatchIn(KotlinSource.withoutComments(it.readText())) }
        assertEquals(listOf("LecternReaderActivity.kt"), overriding.map { it.name })
        assertTrue("the reader follows the user's rotation", reader.contains("ActivityInfo.SCREEN_ORIENTATION_FULL_USER"))

        val base = code("BaseActivity.kt")
        assertTrue("every other screen keeps portrait", base.contains("screenOrientation: Int get() = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT"))
        assertTrue(base.contains("requestedOrientation = screenOrientation"))
        val writers = sources.filter { orientationWrite.containsMatchIn(KotlinSource.withoutComments(it.readText())) }
        assertEquals("only BaseActivity sets the orientation", listOf("BaseActivity.kt"), writers.map { it.name })
    }

    @Test
    fun theReaderKeepsTheScreenOnAndTakesThePageTurnKeys() {
        val onCreate = bodyOf(reader, "override fun onCreate(") ?: error("the reader has no onCreate")
        assertTrue(onCreate.contains("addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)"))
        val keys = bodyOf(reader, "override fun dispatchKeyEvent(") ?: error("the reader reads no keys")
        assertTrue("the keys are LecternPageTurns'", keys.contains("LecternPageTurns.forKey(") && keys.contains("LecternPageTurns.turnsOn("))
        assertTrue("anything else goes on to the system", keys.contains("super.dispatchKeyEvent(event)"))
    }

    @Test
    fun onStopSaysWhetherItIsOnlyARotation() {
        val onStop = bodyOf(reader, "override fun onStop(") ?: error("the reader has no onStop")
        assertTrue(onStop.contains("viewModel.onScreenStopped(changingConfigurations = isChangingConfigurations)"))
        assertTrue(onStop.contains("super.onStop()"))
        val onStart = bodyOf(reader, "override fun onStart(") ?: error("the reader has no onStart")
        assertTrue(onStart.contains("viewModel.onScreenStarted()"))
    }

    @Test
    fun bothScreensArePrivateAndNoStoragePermissionIsAsked() {
        val manifest = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }.newDocumentBuilder()
            .parse(File(KotlinSource.srcDir, "main/AndroidManifest.xml"))
        fun elements(tag: String): List<Element> =
            manifest.getElementsByTagName(tag).let { nodes -> (0 until nodes.length).map { nodes.item(it) as Element } }

        listOf(".lectern.LecternActivity", ".lectern.LecternReaderActivity").forEach { name ->
            val activity = elements("activity").singleOrNull { it.getAttributeNS(androidNs, "name") == name }
                ?: throw AssertionError("$name is not declared")
            assertEquals("$name exported", "false", activity.getAttributeNS(androidNs, "exported"))
            // The orientation is the code's (BaseActivity); a manifest value would only fight it.
            assertEquals("$name screenOrientation", "", activity.getAttributeNS(androidNs, "screenOrientation"))
        }
        val permissions = elements("uses-permission").map { it.getAttributeNS(androidNs, "name") }
        val storage = permissions.filter { it.contains("STORAGE") || it.startsWith("android.permission.READ_MEDIA") }
        assertEquals("the system picker needs no storage permission", emptyList<String>(), storage)
    }

    @Test
    fun theChecksCanFail() {
        // Negative controls: the shapes these checks reject must be rejected.
        assertTrue(orientationOverride.containsMatchIn("override val screenOrientation: Int get() = 0"))
        assertFalse(orientationOverride.containsMatchIn(KotlinSource.withoutComments("// override val screenOrientation")))
        assertTrue(orientationWrite.containsMatchIn("requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR"))
        val withoutOnStop = "class A {\n    override fun onStart() {\n        super.onStart()\n    }\n}"
        assertNull(bodyOf(withoutOnStop, "override fun onStop("))
        val onStopThatForgets = "class A {\n    override fun onStop() {\n        super.onStop()\n    }\n\n    fun other() {}\n}"
        assertFalse(bodyOf(onStopThatForgets, "override fun onStop(")!!.contains("viewModel.onScreenStopped("))
    }
}
