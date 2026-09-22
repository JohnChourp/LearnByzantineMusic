package com.johnchourp.learnbyzantinemusic.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ClickUp `869f4tpju`: the mapping from what the user chose to what they see.
 *
 * This is the piece that decides the whole appearance, and it was the one mutation the contrast
 * tests could not catch — they check that the palettes are good, not that the right one is picked.
 */
class AppThemeModeTest {

    @Test
    fun anExplicitChoiceOverridesTheDevice() {
        // The entire reason for offering the choice: this app is used in a dark church by people
        // whose phone is set to light, and the other way round.
        assertTrue(AppThemeMode.DARK.isDark(systemIsDark = false))
        assertTrue(AppThemeMode.HIGH_CONTRAST.isDark(systemIsDark = false))
        assertEquals(false, AppThemeMode.LIGHT.isDark(systemIsDark = true))
    }

    @Test
    fun systemFollowsTheDeviceAndIsTheOnlyModeThatDoes() {
        assertTrue(AppThemeMode.SYSTEM.isDark(systemIsDark = true))
        assertEquals(false, AppThemeMode.SYSTEM.isDark(systemIsDark = false))

        val followsDevice = AppThemeMode.entries.filter {
            it.isDark(systemIsDark = true) != it.isDark(systemIsDark = false)
        }
        assertEquals(listOf(AppThemeMode.SYSTEM), followsDevice)
    }

    @Test
    fun eachModeResolvesToTheExpectedPalette() {
        assertSame(LbmPalette.light, AppThemeMode.LIGHT.palette(systemIsDark = true))
        assertSame(LbmPalette.dark, AppThemeMode.DARK.palette(systemIsDark = false))
        assertSame(LbmPalette.highContrast, AppThemeMode.HIGH_CONTRAST.palette(systemIsDark = false))
        assertSame(LbmPalette.light, AppThemeMode.SYSTEM.palette(systemIsDark = false))
        assertSame(LbmPalette.dark, AppThemeMode.SYSTEM.palette(systemIsDark = true))
    }

    @Test
    fun highContrastIsNeverSilentlyDowngradedToPlainDark() {
        // It is a distinct accessibility choice, not "dark, but more so on some devices".
        listOf(true, false).forEach { systemIsDark ->
            assertSame(
                LbmPalette.highContrast,
                AppThemeMode.HIGH_CONTRAST.palette(systemIsDark),
            )
        }
    }

    @Test
    fun theStoredSpellingIsFrozen() {
        // These strings sit in app_theme_mode. Renaming one silently resets everyone's choice.
        assertEquals(
            listOf("system", "light", "dark", "high_contrast"),
            AppThemeMode.entries.map { it.storedValue },
        )
    }

    @Test
    fun anUnknownOrMissingStoredValueFallsBackRatherThanCrashingALaunch() {
        // This is read in attachBaseContext, before anything is on screen: throwing here would be
        // a launch crash with no UI to report it.
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.fromStored(null))
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.fromStored(""))
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.fromStored("midnight"))
        assertEquals(AppThemeMode.DEFAULT, AppThemeMode.fromStored("nonsense"))
    }

    @Test
    fun everyStoredValueRoundTrips() {
        AppThemeMode.entries.forEach { mode ->
            assertEquals(mode, AppThemeMode.fromStored(mode.storedValue))
        }
    }

    @Test
    fun theDefaultFollowsTheDevice() {
        // Most people set this once, device-wide, and expect apps to honour it.
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.DEFAULT)
    }
}
