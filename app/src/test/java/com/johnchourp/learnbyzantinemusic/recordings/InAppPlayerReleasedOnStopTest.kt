package com.johnchourp.learnbyzantinemusic.recordings

import com.johnchourp.learnbyzantinemusic.docs.KotlinSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * No sound in the background (ClickUp `869f5x268`): every screen with the in-app player releases it
 * in `onStop`. The release itself is `PlayerController.suspend`, pinned by `PlayerLifecycleTest`; this
 * checks the one line in each screen no JVM test can run — the call from `onStop`. Comments are
 * stripped with [KotlinSource], and only the body of `onStop` is read.
 */
class InAppPlayerReleasedOnStopTest {

    private val screens = listOf(
        "recordings/RecordingsActivity.kt",
        "recordings/RecordingsManagerActivity.kt",
        "anastasimatarion/HymnActivity.kt",
    )

    private fun code(path: String): String =
        KotlinSource.withoutComments(File(KotlinSource.mainRoot, "com/johnchourp/learnbyzantinemusic/$path").readText())

    /** From `override fun onStop(` to the next class-level member; null when there is none. */
    private fun onStopBody(code: String): String? {
        val start = code.indexOf("override fun onStop(")
        if (start < 0) return null
        val rest = code.substring(start)
        val next = Regex("""\n {4}(private |override |protected |internal )*(fun|val|var|companion|class|enum|object) """)
            .find(rest, startIndex = 1)
        return if (next != null) rest.substring(0, next.range.first) else rest
    }

    @Test
    fun everyScreenWithThePlayerReleasesItInOnStop() {
        val missing = screens.filter { path ->
            val code = code(path)
            val hasPlayer = code.contains("private val player: InAppPlayerViewModel by viewModels()")
            val body = onStopBody(code)
            !hasPlayer || body == null || !body.contains("player.onScreenStopped()")
        }
        assertEquals("these screens can keep playing in the background", emptyList<String>(), missing)
    }

    @Test
    fun theReleaseIsInOnStopNotSomewhereLater() {
        // onDestroy may never come for a screen left in the background; onStop always does.
        screens.forEach { path ->
            val body = onStopBody(code(path)) ?: error("$path has no onStop")
            assertTrue("$path: the slice is onStop itself", body.startsWith("override fun onStop("))
            assertTrue("$path: and it calls super", body.contains("super.onStop()"))
        }
    }

    @Test
    fun theCheckNoticesAScreenWithoutIt() {
        // Negative control: a screen without onStop, and one whose onStop does something else.
        assertNull(onStopBody("class X {\n    override fun onResume() {\n        super.onResume()\n    }\n}\n"))
        val other = onStopBody("class X {\n    override fun onStop() {\n        super.onStop()\n    }\n\n    private fun play() = player.onScreenStopped()\n}\n")
        assertTrue(other != null && !other.contains("player.onScreenStopped()"))
    }
}
