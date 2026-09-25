package com.johnchourp.learnbyzantinemusic.recordings

import com.johnchourp.learnbyzantinemusic.docs.KotlinSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The declarations the recording's foreground service stands on (ClickUp `869f5x273`), asserted
 * because each is invisible in a diff of the Kotlin and each fails only on a device:
 *
 * - no `foregroundServiceType="microphone"` → on Android 11+ the service records silence, and on 14+
 *   `startForeground` throws;
 * - no `FOREGROUND_SERVICE_MICROPHONE` → on 14+ it throws too;
 * - `exported` not false → any app could start or stop someone's recording;
 * - `stopWithTask="true"` → swiping the app away would never reach `onTaskRemoved`, which saves.
 *
 * The manifest is parsed as XML, so a comment mentioning a permission cannot satisfy the check.
 */
class RecordingServiceManifestTest {

    private val androidNs = "http://schemas.android.com/apk/res/android"

    private val manifest by lazy {
        DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(File(KotlinSource.srcDir, "main/AndroidManifest.xml"))
    }

    private fun elements(tag: String): List<Element> =
        manifest.getElementsByTagName(tag).let { nodes -> (0 until nodes.length).map { nodes.item(it) as Element } }

    private fun Element.android(attribute: String): String? =
        getAttributeNS(androidNs, attribute).takeIf { it.isNotEmpty() }

    private val service: Element? by lazy {
        elements("service").singleOrNull { it.android("name") == ".recordings.session.RecordingService" }
    }

    @Test
    fun theManifestIsReallyRead() {
        // Guards the slice: a parse of the wrong file would make the absence checks below trivial.
        assertTrue("expected the app's screens", elements("activity").size >= 20)
    }

    @Test
    fun theServiceIsDeclaredPrivateAndOfTypeMicrophone() {
        val declared = service ?: throw AssertionError("RecordingService is not declared in the manifest")
        assertEquals("exported", "false", declared.android("exported"))
        assertEquals("foregroundServiceType", "microphone", declared.android("foregroundServiceType"))
        assertNotEquals("swiping the app away must reach onTaskRemoved", "true", declared.android("stopWithTask"))
    }

    @Test
    fun theDeclaredClassExists() {
        val source = File(KotlinSource.mainRoot, "com/johnchourp/learnbyzantinemusic/recordings/session/RecordingService.kt")
        assertTrue("the manifest names a class that is not there", source.isFile)
        assertTrue(source.readText().contains("class RecordingService : Service()"))
    }

    @Test
    fun itsPermissionsAreDeclared() {
        val declared = elements("uses-permission").mapNotNull { it.android("name") }.toSet()
        listOf(
            "android.permission.RECORD_AUDIO",
            "android.permission.FOREGROUND_SERVICE",
            "android.permission.FOREGROUND_SERVICE_MICROPHONE",
            "android.permission.POST_NOTIFICATIONS",
        ).forEach { permission ->
            assertTrue("missing $permission", permission in declared)
        }
    }
}
