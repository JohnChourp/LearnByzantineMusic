package com.johnchourp.learnbyzantinemusic.ui.theme

import android.content.Context
import android.content.res.Configuration
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs

/**
 * What the user chose in Ρυθμίσεις (ClickUp `869f4tpju`).
 *
 * [SYSTEM] is the default because most people set this once, device-wide, and expect apps to honour
 * it. The explicit choices exist because this app is used in a specific place — a dimly lit
 * ἀναλόγιο — where someone may want dark here and light everywhere else.
 */
enum class AppThemeMode(val storedValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark"),
    HIGH_CONTRAST("high_contrast");

    companion object {
        val DEFAULT = SYSTEM

        /** Boundary parser: an unknown stored value falls back rather than crashing a launch. */
        fun fromStored(value: String?): AppThemeMode =
            entries.firstOrNull { it.storedValue == value } ?: DEFAULT

        fun saved(context: Context): AppThemeMode = fromStored(
            AppPrefs.open(context, AppPrefs.Store.SETTINGS)
                .getString(AppPrefs.ThemeMode.name, DEFAULT.storedValue)
        )

        fun save(context: Context, mode: AppThemeMode) {
            AppPrefs.open(context, AppPrefs.Store.SETTINGS)
                .edit()
                .putString(AppPrefs.ThemeMode.name, mode.storedValue)
                .apply()
        }
    }

    /**
     * Whether this choice paints dark, given what the device currently reports.
     *
     * [SYSTEM] is the only mode that consults the device; the rest are the user overriding it, which
     * is the entire point of offering them.
     */
    fun isDark(systemIsDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemIsDark
        LIGHT -> false
        DARK, HIGH_CONTRAST -> true
    }

    /** The palette this choice paints with. */
    fun palette(systemIsDark: Boolean): LbmPalette = when (this) {
        HIGH_CONTRAST -> LbmPalette.highContrast
        else -> if (isDark(systemIsDark)) LbmPalette.dark else LbmPalette.light
    }
}

/** True when the device configuration is currently in night mode. */
fun Configuration.isNightMode(): Boolean =
    (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
