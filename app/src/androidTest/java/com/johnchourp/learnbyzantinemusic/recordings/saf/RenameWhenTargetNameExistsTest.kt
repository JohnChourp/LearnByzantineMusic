package com.johnchourp.learnbyzantinemusic.recordings.saf

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.johnchourp.learnbyzantinemusic.recordings.RecordingDocumentOps
import com.johnchourp.learnbyzantinemusic.recordings.RenameOutcome
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * **Scenario: something already has the name the user typed.**
 *
 * SAF providers do not agree on what happens then — some refuse, some silently make
 * `name (1).m4a`, and at least one overwrites. The guard under test is the `findFile(targetName)`
 * check that stops before any of that and returns [RenameOutcome.NAME_EXISTS].
 *
 * Remove it and the behaviour becomes whatever the provider does, which on a provider that
 * overwrites means **losing the other recording**. This asserts the existing file is still there
 * with its own bytes (ClickUp `869f4tpt9`, B5).
 */
@RunWith(AndroidJUnit4::class)
class RenameWhenTargetNameExistsTest {

    private val occupantBytes = "the other recording".toByteArray()

    @Test
    fun aNameAlreadyTakenIsRefusedBeforeAnythingIsTouched() {
        val root = SafTestFixture.freshRoot("conflict")
        SafTestFixture.writeFile(root, "source.m4a")
        SafTestFixture.writeFile(root, "taken.m4a", occupantBytes)

        val outcome = RecordingDocumentOps.renameFileWithFallback(
            context = SafTestFixture.context,
            sourceUri = SafTestFixture.documentUri("source.m4a"),
            parentFolderUri = SafTestFixture.treeUri(),
            targetName = "taken.m4a",
        )

        assertEquals(RenameOutcome.NAME_EXISTS, outcome)
        assertArrayEquals(
            "the file that already had the name must be untouched",
            occupantBytes,
            File(root, "taken.m4a").readBytes(),
        )
        assertEquals(
            "the source must still be there under its own name",
            true,
            File(root, "source.m4a").exists(),
        )
    }
}
