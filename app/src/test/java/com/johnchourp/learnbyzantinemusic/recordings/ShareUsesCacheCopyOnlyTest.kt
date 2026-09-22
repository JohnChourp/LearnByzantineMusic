package com.johnchourp.learnbyzantinemusic.recordings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * ClickUp `869f4tpmq` (A7), the half that is a security property rather than a feature.
 *
 * Opening a recording may fall back to the original SAF URI, because some external players cope
 * better with the real document and the chooser there is a short-lived view grant. **Sharing must
 * not.** A shared URI goes to an app the user picks, which may keep it, re-share it or upload it —
 * and a SAF tree URI carries a handle to the user's whole folder, not just one file.
 *
 * The behaviour lives in Android framework calls that a unit test cannot exercise, so this asserts
 * the property at the level where it can actually be broken: the source of `shareRecording`. That is
 * a narrow check, and it is scoped deliberately — it reads only that one function's body, so an
 * unrelated mention of `sourceUri` elsewhere in the file cannot make it pass or fail by accident.
 */
class ShareUsesCacheCopyOnlyTest {

    private val openerSource: String by lazy {
        val candidates = listOf(
            File("app/src/main/java/com/johnchourp/learnbyzantinemusic/recordings/RecordingExternalOpener.kt"),
            File("src/main/java/com/johnchourp/learnbyzantinemusic/recordings/RecordingExternalOpener.kt"),
        )
        (candidates.firstOrNull { it.exists() }
            ?: error("RecordingExternalOpener.kt not found from ${File("").absolutePath}")).readText()
    }

    /** The body of `shareRecording`, from its declaration to the start of the next function. */
    private val shareBody: String by lazy {
        val start = openerSource.indexOf("suspend fun shareRecording(")
        assertTrue("shareRecording not found — this test is checking nothing", start >= 0)
        val rest = openerSource.substring(start)
        val nextFun = Regex("""\n {4}(private |suspend )?fun """).find(rest, startIndex = 1)
        if (nextFun != null) rest.substring(0, nextFun.range.first) else rest
    }

    @Test
    fun theSliceIsRealAndBounded() {
        // A slice that missed would make every negative assertion below trivially pass.
        assertTrue("slice is suspiciously short: ${shareBody.length}", shareBody.length > 400)
        assertTrue(shareBody.startsWith("suspend fun shareRecording("))
        assertFalse(
            "the slice must stop before the next function",
            shareBody.contains("fun buildMimeCandidates("),
        )
    }

    @Test
    fun shareBuildsItsUriThroughFileProvider() {
        assertTrue(
            "sharing must hand out our own FileProvider URI",
            shareBody.contains("FileProvider.getUriForFile"),
        )
        assertTrue("sharing must copy into our cache first", shareBody.contains("copyToOpenCache"))
    }

    @Test
    fun shareNeverPutsTheOriginalSafUriIntoTheIntent() {
        // sourceUri may be READ (it is the copy's source), but must never be handed out.
        listOf(
            "putExtra(Intent.EXTRA_STREAM, sourceUri)",
            "newRawUri(fileName, sourceUri)",
            "setDataAndType(sourceUri",
        ).forEach { leak ->
            assertFalse("share must not expose the SAF URI: $leak", shareBody.contains(leak))
        }
        assertEquals(
            "EXTRA_STREAM must carry exactly one value, the cached URI",
            1,
            Regex("""putExtra\(Intent\.EXTRA_STREAM,\s*cachedUri\)""").findAll(shareBody).count(),
        )
    }

    @Test
    fun aFailedCopyRefusesToShareRatherThanFallingBack() {
        assertTrue(
            "a failed cache copy must return false, not fall back to the SAF URI",
            shareBody.contains("getOrElse") && shareBody.contains("return false"),
        )
    }

    @Test
    fun openingKeepsItsSafFallbackSoTheTwoPathsStayDifferent() {
        // Guards against "fixing" this by removing the open path's fallback: that would be a
        // behaviour regression for external players, and would make the share assertion meaningless
        // because the two paths would no longer differ.
        val openStart = openerSource.indexOf("suspend fun openRecordingWithChooser(")
        assertTrue(openStart >= 0)
        val openBody = openerSource.substring(openStart, openerSource.indexOf("private fun buildMimeCandidates("))
        assertTrue(
            "opening still tries the original URI as well as the cached one",
            openBody.contains("sourceUri,") && openBody.contains("cachedUri"),
        )
    }
}
