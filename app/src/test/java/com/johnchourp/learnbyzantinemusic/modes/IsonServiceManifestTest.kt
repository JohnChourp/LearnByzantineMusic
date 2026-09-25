package com.johnchourp.learnbyzantinemusic.modes

import com.johnchourp.learnbyzantinemusic.docs.KotlinSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * What the background ison stands on (ClickUp `869f5x2dq`), asserted because each part is invisible
 * in a diff of the Kotlin and each fails only on a device:
 *
 * - no `foregroundServiceType="mediaPlayback"`, or no `FOREGROUND_SERVICE_MEDIA_PLAYBACK` → on
 *   Android 14 `startForeground` throws, and the ison never leaves the page;
 * - `exported` not false → any app could start or stop the ison;
 * - `startForegroundService` anywhere → a refused foreground kills the whole process instead of
 *   falling back to the page (the rule `RecordingService` set, and this service follows);
 * - audio focus → the ison would silence a recording, or be silenced by it;
 * - the microphone → «Πού είμαι» must stay with the visible page.
 *
 * The manifest is parsed as XML and the Kotlin is read without its comments, so a comment naming a
 * permission or an API can neither satisfy nor trip a check.
 */
class IsonServiceManifestTest {

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

    private fun code(name: String): String {
        val file = File(KotlinSource.mainRoot, "com/johnchourp/learnbyzantinemusic/modes/$name")
        assertTrue("$file is missing", file.isFile)
        return KotlinSource.withoutComments(file.readText())
    }

    private val service by lazy { code("IsonPlaybackService.kt") }
    private val page by lazy { code("EightModesActivity.kt") }
    private val player by lazy { code("PhthongTonePlayer.kt") }

    private val startsInTheForeground = Regex("""\b(startForegroundService|getForegroundService)\s*\(""")
    private val audioFocus = Regex("""\b(requestAudioFocus|AudioFocusRequest)\b""")
    private val microphone = Regex("""\b(AudioRecord|TrainerPitchEngine|RECORD_AUDIO)\b""")
    private val ownChannel = Regex("""\bNotificationChannel(Compat)?\s*(\.Builder)?\s*\(""")

    // ---- the manifest ---------------------------------------------------------------------------

    @Test
    fun theManifestIsReallyRead() {
        // Guards the slice: a parse of the wrong file would make the checks below trivial.
        assertTrue("expected the app's screens", elements("activity").size >= 20)
    }

    @Test
    fun theServiceIsDeclaredPrivateAndOfTypeMediaPlayback() {
        val declared = elements("service").singleOrNull { it.android("name") == ".modes.IsonPlaybackService" }
            ?: throw AssertionError("IsonPlaybackService is not declared in the manifest")
        assertEquals("exported", "false", declared.android("exported"))
        assertEquals("foregroundServiceType", "mediaPlayback", declared.android("foregroundServiceType"))
    }

    @Test
    fun itsPermissionsAreDeclaredEachOnce() {
        val permissions = elements("uses-permission").mapNotNull { it.android("name") }
        listOf(
            "android.permission.FOREGROUND_SERVICE",
            "android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK",
            "android.permission.POST_NOTIFICATIONS",
        ).forEach { permission -> assertTrue("$permission is not declared", permission in permissions) }
        // The recording's service shares two of them: one declaration each, not one per feature.
        assertEquals("a permission declared twice", permissions.distinct(), permissions)
    }

    // ---- the code -------------------------------------------------------------------------------

    @Test
    fun itGoesToTheForegroundAsMediaPlayback() {
        assertTrue(service.contains("ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK"))
    }

    @Test
    fun nothingStartsItWithStartForegroundService() {
        assertFalse("the service", startsInTheForeground.containsMatchIn(service))
        assertFalse("the page", startsInTheForeground.containsMatchIn(page))
    }

    @Test
    fun itNeverAsksForAudioFocusAndNeverTouchesTheMicrophone() {
        assertFalse("audio focus in the service", audioFocus.containsMatchIn(service))
        assertFalse("audio focus in the player", audioFocus.containsMatchIn(player))
        assertFalse("the microphone in the service", microphone.containsMatchIn(service))
    }

    @Test
    fun itsNotificationGoesThroughTheSharedHelper() {
        assertTrue(service.contains("AppNotifications.Channel.ISON"))
        assertTrue(service.contains("AppNotifications.ISON_NOTIFICATION_ID"))
        assertFalse("a channel of its own: channels are AppNotifications'", ownChannel.containsMatchIn(service))
    }

    @Test
    fun theSourceChecksCanFail() {
        // Negative control: each pattern matches the shape it rejects, or its absence above proves nothing.
        assertTrue(startsInTheForeground.containsMatchIn("ContextCompat.startForegroundService(context, intent)"))
        assertTrue(startsInTheForeground.containsMatchIn("PendingIntent.getForegroundService(this, 1, intent, flags)"))
        assertTrue(audioFocus.containsMatchIn("audioManager.requestAudioFocus(request)"))
        assertTrue(microphone.containsMatchIn("val record = AudioRecord(source, rate, mask, format, size)"))
        assertTrue(ownChannel.containsMatchIn("manager.createNotificationChannel(NotificationChannel(id, name, importance))"))
        assertFalse(startsInTheForeground.containsMatchIn("context.startService(intent)"))
    }
}
