package com.johnchourp.learnbyzantinemusic.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ClickUp `869f4tpju`: the mapping from what the user chose to what they see — and, since v1.17.4,
 * the rule that the user always chooses (operator decision, 2026-09-26).
 *
 * This is the piece that decides the whole appearance, and it was the one mutation the contrast
 * tests could not catch — they check that the palettes are good, not that the right one is picked.
 */
class AppThemeModeTest {

    @Test
    fun theChoicesAreLightDarkAndHighContrastOnly() {
        // «Όπως η συσκευή» is gone: nothing Ρυθμίσεις offers changes by itself.
        assertEquals(
            listOf(AppThemeMode.LIGHT, AppThemeMode.DARK, AppThemeMode.HIGH_CONTRAST),
            AppThemeMode.entries,
        )
    }

    @Test
    fun anExplicitChoiceIgnoresTheDevice() {
        // The reason for the choice: this app is used in a dark church by people whose phone is set
        // to light, and the other way round.
        AppThemeMode.entries.forEach { mode ->
            listOf(true, false).forEach { systemIsDark ->
                listOf(true, false).forEach { appInUse ->
                    assertEquals(
                        "$mode, device dark = $systemIsDark, app in use = $appInUse",
                        mode,
                        AppThemeMode.resolve(mode.storedValue, systemIsDark, appInUse),
                    )
                }
            }
        }
        assertTrue(AppThemeMode.DARK.isDark)
        assertTrue(AppThemeMode.HIGH_CONTRAST.isDark)
        assertFalse(AppThemeMode.LIGHT.isDark)
    }

    @Test
    fun whoFollowedTheDeviceKeepsWhatTheDeviceShowsNow() {
        // v1.16.0–v1.17.3 stored «system» for «Όπως η συσκευή»; the update must not change anyone's
        // screen, so it becomes the light or dark on screen at that moment.
        assertEquals(AppThemeMode.DARK, AppThemeMode.resolve("system", systemIsDark = true, appInUse = true))
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.resolve("system", systemIsDark = false, appInUse = true))
        // An old «Δεδομένα μάθησης» file brings «system» even to a phone where the app is new.
        assertEquals(AppThemeMode.DARK, AppThemeMode.resolve("system", systemIsDark = true, appInUse = false))
    }

    @Test
    fun anAppInUseThatNeverChoseFollowedTheDeviceToo() {
        // Nothing stored was the old default, «Όπως η συσκευή», for anyone who never opened the choice.
        assertEquals(AppThemeMode.DARK, AppThemeMode.resolve(null, systemIsDark = true, appInUse = true))
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.resolve(null, systemIsDark = false, appInUse = true))
    }

    @Test
    fun aNewInstallStartsLightWhateverTheDeviceShows() {
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.resolve(null, systemIsDark = true, appInUse = false))
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.resolve(null, systemIsDark = false, appInUse = false))
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.DEFAULT)
    }

    @Test
    fun eachModeResolvesToTheExpectedPalette() {
        assertSame(LbmPalette.light, AppThemeMode.LIGHT.palette)
        assertSame(LbmPalette.dark, AppThemeMode.DARK.palette)
        // A distinct accessibility choice, never "dark, but more so on some devices".
        assertSame(LbmPalette.highContrast, AppThemeMode.HIGH_CONTRAST.palette)
    }

    @Test
    fun theStoredSpellingIsFrozen() {
        // These strings sit in app_theme_mode. Renaming one silently resets everyone's choice — and
        // «system» is still on phones and in files until each is read once.
        assertEquals(listOf("light", "dark", "high_contrast"), AppThemeMode.entries.map { it.storedValue })
        assertEquals("system", AppThemeMode.LEGACY_FOLLOW_DEVICE)
    }

    @Test
    fun anUnknownStoredValueFallsBackRatherThanCrashingALaunch() {
        // This is read in attachBaseContext, before anything is on screen: throwing here would be
        // a launch crash with no UI to report it.
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.fromStored(null))
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.fromStored(""))
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.resolve("midnight", systemIsDark = true, appInUse = true))
    }

    @Test
    fun everyStoredValueRoundTrips() {
        AppThemeMode.entries.forEach { mode ->
            assertEquals(mode, AppThemeMode.fromStored(mode.storedValue))
        }
    }
}
