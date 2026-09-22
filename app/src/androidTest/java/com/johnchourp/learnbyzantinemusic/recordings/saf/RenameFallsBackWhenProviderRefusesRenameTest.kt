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
 * **Scenario: the provider has no rename, so the copy/delete fallback has to carry it.**
 *
 * This is the reason `renameFileWithFallback` is not three lines. Several real providers either
 * omit `FLAG_SUPPORTS_RENAME` or throw from `renameDocument`, and a rename that simply reported
 * failure there would make renaming impossible on those devices.
 *
 * The fallback must end with **exactly one** file, under the new name, **with the original bytes**.
 * Copying without deleting leaves a duplicate; deleting before the copy is verified loses the
 * recording (ClickUp `869f4tpt9`, B5).
 */
@RunWith(AndroidJUnit4::class)
class RenameFallsBackWhenProviderRefusesRenameTest {

    private val bytes = "the original recording bytes".toByteArray()

    @Test
    fun theProviderReallyIsRefusingRename() {
        // Guards the slice. If the switch did not take effect the direct rename would succeed and
        // this file would be testing the happy path while claiming to test the fallback.
        val root = SafTestFixture.freshRoot("refuse-check")
        SafTestFixture.writeFile(root, "a.m4a", bytes)
        FakeSafProvider.refuseRename = true
        val doc = androidx.documentfile.provider.DocumentFile.fromSingleUri(
            SafTestFixture.context,
            SafTestFixture.documentUri("a.m4a"),
        )!!
        assertFalse("the provider must refuse a direct rename", runCatching { doc.renameTo("b.m4a") }.getOrDefault(false))
        assertTrue("and the original must still be there", File(root, "a.m4a").exists())
    }

    @Test
    fun theFileIsMovedByCopyAndDeleteWithItsBytesIntact() {
        val root = SafTestFixture.freshRoot("fallback")
        SafTestFixture.writeFile(root, "source.m4a", bytes)
        FakeSafProvider.refuseRename = true

        val outcome = RecordingDocumentOps.renameFileWithFallback(
            context = SafTestFixture.context,
            sourceUri = SafTestFixture.documentUri("source.m4a"),
            parentFolderUri = SafTestFixture.treeUri(),
            targetName = "renamed.m4a",
        )

        assertEquals(RenameOutcome.SUCCESS, outcome)
        assertFalse("the source must be gone, not duplicated", File(root, "source.m4a").exists())
        assertTrue("the target must exist", File(root, "renamed.m4a").exists())
        assertArrayEquals("the bytes must survive the copy", bytes, File(root, "renamed.m4a").readBytes())
        assertEquals("exactly one file must remain", 1, root.list()?.size)
    }
}
