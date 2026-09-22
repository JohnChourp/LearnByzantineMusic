package com.johnchourp.learnbyzantinemusic.recordings.saf

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.johnchourp.learnbyzantinemusic.recordings.RecordingDocumentOps
import com.johnchourp.learnbyzantinemusic.recordings.RenameOutcome
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * **Scenario: tree URI vs document URI mismatch, and a folder that is not there.**
 *
 * The two URI shapes are easy to confuse — `DocumentFile.fromTreeUri` on a single-document URI does
 * not fail, it returns a `DocumentFile` that simply is not a directory. The guard under test is
 * `!parentFolder.exists() || !parentFolder.isDirectory`, which stops before `createFile` is asked
 * to make a child of a file.
 *
 * Remove it and the rename runs on against a nonsensical parent: the user is told a rename failed
 * for no stated reason, and on some providers a stray document appears somewhere unexpected
 * (ClickUp `869f4tpt9`, B5).
 */
@RunWith(AndroidJUnit4::class)
class RenameWhenParentIsNotAFolderTest {

    private val bytes = "left alone".toByteArray()

    @Test
    fun aFileHandedInAsTheParentFolderIsRefused() {
        val root = SafTestFixture.freshRoot("parent-is-file")
        SafTestFixture.writeFile(root, "source.m4a", bytes)
        SafTestFixture.writeFile(root, "not_a_folder.m4a")

        val outcome = RecordingDocumentOps.renameFileWithFallback(
            context = SafTestFixture.context,
            sourceUri = SafTestFixture.documentUri("source.m4a"),
            // A document URI where a tree URI belongs — the mismatch the guard is for.
            parentFolderUri = SafTestFixture.documentUri("not_a_folder.m4a"),
            targetName = "renamed.m4a",
        )

        assertEquals(RenameOutcome.FAILED, outcome)
        assertArrayEquals("the source must be untouched", bytes, File(root, "source.m4a").readBytes())
        assertEquals("nothing may have been created", 2, root.list()?.size)
    }

    @Test
    fun aFolderThatIsNoLongerThereIsRefused() {
        // The revoked / moved folder case: the grant is stale and the directory is simply absent.
        val root = SafTestFixture.freshRoot("parent-missing")
        SafTestFixture.writeFile(root, "source.m4a", bytes)

        val outcome = RecordingDocumentOps.renameFileWithFallback(
            context = SafTestFixture.context,
            sourceUri = SafTestFixture.documentUri("source.m4a"),
            parentFolderUri = SafTestFixture.documentUri("folder_that_was_deleted"),
            targetName = "renamed.m4a",
        )

        assertEquals(RenameOutcome.FAILED, outcome)
        assertTrue("the source must be untouched", File(root, "source.m4a").exists())
    }
}
