package com.johnchourp.learnbyzantinemusic.ui.theme

import android.content.Context
import android.content.res.Configuration
import com.johnchourp.learnbyzantinemusic.AppLanguage
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs

/**
 * What the user chose in Ρυθμίσεις (ClickUp `869f4tpju`).
 *
 * **The user always chooses (2026-09-26).** There used to be a fourth choice, «Όπως η συσκευή»
 * (`system`), which followed the device and was the default. The operator decided that the theme
 * never changes by itself: Ρυθμίσεις offers [LIGHT], [DARK] and [HIGH_CONTRAST], and a new install
 * starts [LIGHT]. The explicit choices exist because this app is used in a specific place — a dimly
 * lit ἀναλόγιο — where someone may want dark here and light everywhere else.
 *
 * **Nobody's screen changes on the update.** `system` is what v1.16.0–v1.17.3 stored for «Όπως η
 * συσκευή», what they meant when nothing was stored, and what an older «Δεδομένα μάθησης» file may
 * carry. [saved] turns it, once, into the light or dark the device shows at that moment and stores
 * the result, so from then on the device no longer matters — see [resolve].
 */
enum class AppThemeMode(val storedValue: String) {
    LIGHT("light"),
    DARK("dark"),
    HIGH_CONTRAST("high_contrast");

    companion object {
        /** A new install starts light; the user changes it in Ρυθμίσεις. */
        val DEFAULT = LIGHT

        /** What v1.16.0–v1.17.3 stored for «Όπως η συσκευή». No longer a choice; see [resolve]. */
        const val LEGACY_FOLLOW_DEVICE = "system"

        /** Boundary parser: an unknown stored value falls back rather than crashing a launch. */
        fun fromStored(value: String?): AppThemeMode =
            entries.firstOrNull { it.storedValue == value } ?: DEFAULT

        /**
         * The choice [stored] stands for now.
         *
         * [LEGACY_FOLLOW_DEVICE], or nothing stored in an app already in use ([appInUse] — the old
         * default meant the same), keeps what the device shows right now ([systemIsDark]). A new
         * install starts at [DEFAULT]. Any other value is the user's own choice and ignores the device.
         */
        fun resolve(stored: String?, systemIsDark: Boolean, appInUse: Boolean): AppThemeMode = when {
            stored == LEGACY_FOLLOW_DEVICE || (stored == null && appInUse) -> if (systemIsDark) DARK else LIGHT
            else -> fromStored(stored)
        }

        /**
         * The saved choice. The first read after the update settles a legacy value and stores it.
         * BaseActivity reads it first in attachBaseContext, with the context Android handed in, whose
         * configuration is still the device's own.
         */
        fun saved(context: Context): AppThemeMode {
            val stored = AppPrefs.open(context, AppPrefs.Store.SETTINGS)
                .getString(AppPrefs.ThemeMode.name, null)
            val mode = resolve(
                stored = stored,
                systemIsDark = context.resources.configuration.isNightMode(),
                appInUse = AppLanguage.isLanguageOnboardingCompleted(context),
            )
            if (stored != mode.storedValue) save(context, mode)
            return mode
        }

        fun save(context: Context, mode: AppThemeMode) {
            AppPrefs.open(context, AppPrefs.Store.SETTINGS)
                .edit()
                .putString(AppPrefs.ThemeMode.name, mode.storedValue)
                .apply()
        }
    }

    /** Whether this choice paints dark. No choice consults the device. */
    val isDark: Boolean get() = this != LIGHT

    /** The palette this choice paints with. */
    val palette: LbmPalette
        get() = when (this) {
            LIGHT -> LbmPalette.light
            DARK -> LbmPalette.dark
            HIGH_CONTRAST -> LbmPalette.highContrast
        }
}

/** True when the device configuration is currently in night mode. */
fun Configuration.isNightMode(): Boolean =
    (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
