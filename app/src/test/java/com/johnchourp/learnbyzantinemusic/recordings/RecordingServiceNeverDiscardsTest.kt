package com.johnchourp.learnbyzantinemusic.recordings

import com.johnchourp.learnbyzantinemusic.docs.KotlinSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Nothing outside the «Ηχογραφήσεις» screen may throw a recording away (ClickUp `869f5x273`). The
 * foreground service, its notification and their buttons can pause, resume and stop-and-save —
 * «Στάση» and a swipe from recents included — but never discard.
 *
 * Two guards, because they fail differently: the service reaches the session only through
 * [com.johnchourp.learnbyzantinemusic.recordings.session.RecordingControls], which has no discard, so
 * the compiler refuses it there; and this reads the files themselves (comments stripped by
 * [KotlinSource]) so a `RecordingSessions.get(…).discard()` added around that type is caught too.
 */
class RecordingServiceNeverDiscardsTest {

    private val sessionDir = File(KotlinSource.mainRoot, "com/johnchourp/learnbyzantinemusic/recordings/session")

    private val serviceFiles = listOf(
        "RecordingService.kt",
        "RecordingServiceCore.kt",
        "RecordingNotification.kt",
        "RecordingNotificationAction.kt",
    )

    private fun code(name: String): String = KotlinSource.withoutComments(File(sessionDir, name).readText())

    private val discardCall = Regex("""\bdiscard\s*\(""")

    @Test
    fun theFilesAreReallyRead() {
        // Guards the slice: a missing or empty file would make the absence check below pass.
        serviceFiles.forEach { name ->
            assertTrue("$name not found", File(sessionDir, name).isFile)
            assertTrue("$name is suspiciously short", code(name).length > 800)
        }
    }

    @Test
    fun noneOfThemCanThrowARecordingAway() {
        assertEquals(
            "the service and its notification must never discard a recording",
            emptyList<String>(),
            serviceFiles.filter { discardCall.containsMatchIn(code(it)) },
        )
    }

    @Test
    fun theServiceReachesTheSessionOnlyThroughItsControls() {
        assertTrue(code("RecordingServiceCore.kt").contains("private val session: RecordingControls,"))
        assertFalse(
            "RecordingControls must not grow a discard",
            discardCall.containsMatchIn(code("RecordingNotificationAction.kt")),
        )
    }

    @Test
    fun theCheckCatchesADiscardAndNotACommentAboutOne() {
        // Negative control: the pattern sees a real call in any spacing, and not the prose explaining it.
        assertTrue(discardCall.containsMatchIn("RecordingSessions.get(this).discard()"))
        assertTrue(discardCall.containsMatchIn("session.discard ()"))
        assertFalse(discardCall.containsMatchIn(KotlinSource.withoutComments("// never discard() here\nval x = 1")))
        assertFalse(discardCall.containsMatchIn("val discardable = false"))
    }
}
