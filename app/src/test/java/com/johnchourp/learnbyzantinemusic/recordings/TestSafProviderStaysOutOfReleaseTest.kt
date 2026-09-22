package com.johnchourp.learnbyzantinemusic.recordings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The SAF test provider must never reach a shipped build (ClickUp `869f4tpt9`, B5).
 *
 * `FakeSafProvider` is declared `exported="true"` — `DocumentsProvider.attachInfo` refuses to start
 * otherwise — and only the `src/debug` source set keeps it out of the release manifest. That is a
 * build-configuration fact, invisible in any diff of the shipped code, so it is asserted rather
 * than trusted.
 */
class TestSafProviderStaysOutOfReleaseTest {

    private val authority = "com.johnchourp.learnbyzantinemusic.test.saf"

    private fun repoFile(path: String): File =
        listOf(File("app/$path"), File(path)).firstOrNull { it.exists() }
            ?: error("$path not found from ${File("").absolutePath}")

    @Test
    fun theFixtureFilesAreWhereThisTestThinksTheyAre() {
        // Guards the slice: if the paths were wrong, every assertion below would pass over nothing.
        assertTrue(repoFile("src/main/AndroidManifest.xml").readText().contains("<manifest"))
        assertTrue(
            "the debug manifest must exist, or the provider is not declared at all",
            repoFile("src/debug/AndroidManifest.xml").readText().contains(authority),
        )
    }

    @Test
    fun theMainManifestDoesNotDeclareIt() {
        assertFalse(
            "the test provider must not be in the shipped manifest",
            repoFile("src/main/AndroidManifest.xml").readText().contains(authority),
        )
    }

    @Test
    fun itsSourceLivesOnlyInTheDebugSourceSet() {
        assertTrue(repoFile("src/debug/java/com/johnchourp/learnbyzantinemusic/recordings/saf/FakeSafProvider.kt").exists())
        assertFalse(
            "a copy under src/main would ship it",
            File(repoFile("src/main/java"), "com/johnchourp/learnbyzantinemusic/recordings/saf/FakeSafProvider.kt").exists(),
        )
    }
}
