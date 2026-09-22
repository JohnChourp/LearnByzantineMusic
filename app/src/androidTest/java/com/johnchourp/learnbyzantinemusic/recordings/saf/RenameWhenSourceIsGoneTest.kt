package com.johnchourp.learnbyzantinemusic.recordings.saf

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.johnchourp.learnbyzantinemusic.recordings.RecordingDocumentOps
import com.johnchourp.learnbyzantinemusic.recordings.RenameOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * **Scenario: the file was deleted or moved out of the folder behind the app's back.**
 *
 * The user's file manager, a sync client or another app can remove a recording between the moment
 * the list was drawn and the moment a rename is confirmed. The guard under test is the
 * `!sourceDocument.exists()` check, which must answer [RenameOutcome.REMOVED] — a distinct outcome
 * from `FAILED`, because the UI tells the user the recording is gone rather than that the rename
 * broke.
 *
 * Remove that check and the code walks on to the copy fallback, which creates an EMPTY target file
 * in the folder: a rename of a vanished recording would leave a 0-byte ghost behind. That is the
 * regression this file exists to catch.
 */
@RunWith(AndroidJUnit4::class)
class RenameWhenSourceIsGoneTest {

    @Test
    fun theFixtureIsRealBeforeAnythingIsAsserted() {
        // Guards the slice: if the provider were not serving, every outcome below would be FAILED
        // for the wrong reason and the test would look like it was proving the guard.
        val root = SafTestFixture.freshRoot("present")
        SafTestFixture.writeFile(root, "recording_20250101_101010.m4a")
        val outcome = RecordingDocumentOps.renameFileWithFallback(
            context = SafTestFixture.context,
            sourceUri = SafTestFixture.documentUri("recording_20250101_101010.m4a"),
            parentFolderUri = SafTestFixture.treeUri(),
            targetName = "renamed.m4a",
        )
        assertEquals("a plain rename must work, or this file proves nothing", RenameOutcome.SUCCESS, outcome)
    }

    @Test
    fun aMissingSourceIsReportedAsRemovedRatherThanFailed() {
        SafTestFixture.freshRoot("gone")
        val outcome = RecordingDocumentOps.renameFileWithFallback(
            context = SafTestFixture.context,
            sourceUri = SafTestFixture.documentUri("never_existed.m4a"),
            parentFolderUri = SafTestFixture.treeUri(),
            targetName = "renamed.m4a",
        )
        assertEquals(RenameOutcome.REMOVED, outcome)
    }

    @Test
    fun aVanishedSourceLeavesNoEmptyGhostBehind() {
        // The consequence of removing the guard, asserted directly on the folder.
        val root = SafTestFixture.freshRoot("ghost")
        val outcome = RecordingDocumentOps.renameFileWithFallback(
            context = SafTestFixture.context,
            sourceUri = SafTestFixture.documentUri("vanished.m4a"),
            parentFolderUri = SafTestFixture.treeUri(),
            targetName = "renamed.m4a",
        )
        assertEquals(RenameOutcome.REMOVED, outcome)
        assertTrue(
            "the folder must stay empty, found ${root.list()?.toList()}",
            root.list()?.isEmpty() == true,
        )
    }
}
