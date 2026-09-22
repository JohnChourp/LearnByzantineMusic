package com.johnchourp.learnbyzantinemusic.recordings.saf

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

/**
 * Shared plumbing for the SAF edge-case tests (ClickUp `869f4tpt9`, B5).
 *
 * Only setup lives here. Each scenario keeps its **own test file** — the acceptance criterion asks
 * for one file per topic rather than a monolith suite, so that a failure names the scenario.
 */
object SafTestFixture {

    /**
     * The **instrumentation** context, not `targetContext`.
     *
     * [FakeSafProvider] is declared in the androidTest manifest, so it belongs to the test package
     * and runs under its UID. It is `exported="false"`, which is a UID check — the app under test
     * has a different package name and therefore a different UID, so resolving these URIs through
     * `targetContext` fails on every call and every outcome comes back `FAILED`. Measured on
     * 2026-09-22: that is exactly what happened, and the slice guard in each file is what said so
     * instead of letting the guard assertions look satisfied.
     *
     * `RecordingDocumentOps` only needs a `ContentResolver`, so the test context is the right one.
     */
    /**
     * The **target** application's context.
     *
     * Instrumentation runs inside the app under test, under its UID, so this is the context whose
     * process actually hosts [FakeSafProvider] — which is why the provider is declared in the app's
     * debug variant and not in the androidTest manifest. Measured 2026-09-22: with the provider in
     * androidTest, nothing was reachable and the test package's own cache was not writable.
     */
    val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    /** A fresh directory served by [FakeSafProvider], with the refusal switches back off. */
    fun freshRoot(name: String): File {
        val root = File(context.cacheDir, "saf-$name-${System.nanoTime()}")
        root.deleteRecursively()
        check(root.mkdirs()) { "could not create $root" }
        FakeSafProvider.reset(root)
        return root
    }

    /** The tree URI for the provider's root — what a folder grant would hand the app. */
    fun treeUri(): Uri = DocumentsContract.buildTreeDocumentUri(
        FakeSafProvider.AUTHORITY,
        FakeSafProvider.ROOT_ID,
    )

    /** The single-document URI of a file inside the root. */
    fun documentUri(fileName: String): Uri = DocumentsContract.buildDocumentUri(
        FakeSafProvider.AUTHORITY,
        "${FakeSafProvider.ROOT_ID}/$fileName",
    )

    /** Creates a real file with real bytes, so a copy has something to copy. */
    fun writeFile(root: File, name: String, bytes: ByteArray = "recording".toByteArray()): File =
        File(root, name).apply { writeBytes(bytes) }
}
