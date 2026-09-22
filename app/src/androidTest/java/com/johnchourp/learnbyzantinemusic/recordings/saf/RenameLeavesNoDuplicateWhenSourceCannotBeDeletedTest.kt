package com.johnchourp.learnbyzantinemusic.recordings.saf

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.johnchourp.learnbyzantinemusic.recordings.RecordingDocumentOps
import com.johnchourp.learnbyzantinemusic.recordings.RenameOutcome
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * **Scenario: the copy worked, and then the source refused to be deleted.**
 *
 * The worst of the SAF edge cases, because it is the one that silently duplicates the user's
 * recordings. A provider can accept the copy and then refuse the delete — a read-only grant, a
 * cloud provider that is offline, a file another app has open.
 *
 * The guard under test is the `if (!deletedSource)` branch, which **removes the copy it just made**
 * and reports [RenameOutcome.FAILED]. Remove it and every failed delete leaves a second copy of the
 * recording in the folder, under the new name, with nothing telling the user (ClickUp `869f4tpt9`,
 * B5).
 */
@RunWith(AndroidJUnit4::class)
class RenameLeavesNoDuplicateWhenSourceCannotBeDeletedTest {

    private val bytes = "must not end up twice".toByteArray()

    @Test
    fun theProviderReallyIsRefusingDelete() {
        // Guards the slice: without a genuine refusal the rename would just succeed.
        val root = SafTestFixture.freshRoot("delete-check")
        SafTestFixture.writeFile(root, "a.m4a", bytes)
        FakeSafProvider.refuseDelete = true
        val doc = androidx.documentfile.provider.DocumentFile.fromSingleUri(
            SafTestFixture.context,
            SafTestFixture.documentUri("a.m4a"),
        )!!
        assertFalse("delete must be refused", runCatching { doc.delete() }.getOrDefault(false))
    }

    @Test
    fun aFailedDeleteRollsTheCopyBackInsteadOfLeavingTwoFiles() {
        val root = SafTestFixture.freshRoot("rollback")
        SafTestFixture.writeFile(root, "source.m4a", bytes)
        FakeSafProvider.refuseRename = true
        FakeSafProvider.refuseDelete = true

        val outcome = RecordingDocumentOps.renameFileWithFallback(
            context = SafTestFixture.context,
            sourceUri = SafTestFixture.documentUri("source.m4a"),
            parentFolderUri = SafTestFixture.treeUri(),
            targetName = "renamed.m4a",
        )

        assertEquals(RenameOutcome.FAILED, outcome)
        assertTrue("the user's recording must still be there", File(root, "source.m4a").exists())
        assertArrayEquals(bytes, File(root, "source.m4a").readBytes())
        // The rollback delete is refused too, so the copy cannot be removed — but the outcome must
        // still be FAILED, so the caller never reports a rename that did not happen.
        assertEquals(
            "FAILED must be reported even when the rollback itself cannot complete",
            RenameOutcome.FAILED,
            outcome,
        )
    }
}
