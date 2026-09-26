package com.johnchourp.learnbyzantinemusic.settings

import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs.Store.SETTINGS
import com.johnchourp.learnbyzantinemusic.settings.LearningDataFile.Result.Accepted
import com.johnchourp.learnbyzantinemusic.settings.LearningDataFile.Result.Rejected
import com.johnchourp.learnbyzantinemusic.ui.theme.AppThemeMode
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * A «Δεδομένα μάθησης» file written by v1.16.0–v1.17.3 may carry the retired «Όπως η συσκευή»,
 * `app_theme_mode = system` (operator decision of 2026-09-26: the user always chooses).
 *
 * The import refuses a **whole file** for a single value the app would not store. So the retired value
 * must still be accepted, and kept as it is: `AppThemeMode.saved` then settles it into what the device
 * shows, exactly as for an app updated in place. Turned into «light» here, a dark phone would switch
 * to light on import; refused, the file would bring back nothing at all.
 */
class LegacyThemeInLearningDataFileTest {

    /** A version 1 file whose only entry is `app_theme_mode`, written by an older build. */
    private fun fileWithTheme(value: String): String = JSONObject()
        .put("schemaVersion", 1)
        .put("exportedAt", 1_758_800_000_000L)
        .put("appVersion", "1.17.3")
        .put(
            "stores",
            JSONObject().put(
                "learn_byzantine_music_settings",
                JSONObject().put("app_theme_mode", JSONObject().put("type", "STRING").put("value", value)),
            ),
        )
        .toString()

    @Test
    fun `a file that followed the device is imported, and keeps the value for the one-time conversion`() {
        val result = LearningDataFile.decode(fileWithTheme(AppThemeMode.LEGACY_FOLLOW_DEVICE))
        assertEquals(
            Accepted(mapOf(SETTINGS to mapOf("app_theme_mode" to "system")), 1_758_800_000_000L, "1.17.3"),
            result,
        )
    }

    @Test
    fun `every choice of today is imported as it is`() {
        AppThemeMode.entries.forEach { mode ->
            val result = LearningDataFile.decode(fileWithTheme(mode.storedValue))
            assertEquals(
                Accepted(mapOf(SETTINGS to mapOf("app_theme_mode" to mode.storedValue)), 1_758_800_000_000L, "1.17.3"),
                result,
            )
        }
    }

    @Test
    fun `a value no version ever stored is still refused`() {
        // The negative control: without it, a decoder that accepted everything would pass both tests above.
        assertEquals(
            Rejected(LearningDataFile.Reason.BAD_VALUE, "app_theme_mode"),
            LearningDataFile.decode(fileWithTheme("sepia")),
        )
    }
}
