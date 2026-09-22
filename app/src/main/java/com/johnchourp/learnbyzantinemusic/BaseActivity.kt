package com.johnchourp.learnbyzantinemusic

import android.content.Context
import android.content.res.Configuration
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.johnchourp.learnbyzantinemusic.ui.theme.AppThemeMode
import com.johnchourp.learnbyzantinemusic.ui.theme.isNightMode

/**
 * The base every screen in the app extends, and the reason two global settings work everywhere
 * without a single screen knowing about them.
 *
 * **Flow.** `attachBaseContext` wraps the incoming context three times before Android inflates
 * anything: with the saved language, then the saved font scale, then the chosen theme's night flag. Because this happens at attach time
 * rather than in `onCreate`, resources resolve in the right locale and at the right scale from the
 * very first layout pass — no screen re-reads a preference, and none can forget to.
 *
 * **Order matters.** Locale first, font scale second: the font-scale wrapper is built on top of the
 * localised configuration, so it cannot drop the locale it was handed.
 *
 * **Also.** Locks every screen to portrait. The app is meant to be read off a stand while chanting,
 * and several diagrams assume portrait width.
 *
 * Reads (never writes): `app_language_code`, `app_font_step`, `app_theme_mode` — see
 * [com.johnchourp.learnbyzantinemusic.prefs.AppPrefs].
 */
abstract class BaseActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context?) {
        val languageContext = AppLanguage.wrapContextWithLocale(newBase)
        val fontScaleContext = AppFontScale.wrapContextWithFontScale(languageContext)
        super.attachBaseContext(wrapContextWithThemeMode(fontScaleContext))
    }

    /**
     * Forces the night flag to match the user's own choice, so the two View-based screens resolve
     * their `values-night` colours from the in-app setting rather than only from the device.
     *
     * Done here, in the same place as locale and font scale, for the same reason: by the time
     * anything inflates, resources must already resolve correctly. A `setDefaultNightMode` call
     * would work too but lives on AppCompat, which these activities deliberately do not extend.
     *
     * [AppThemeMode.SYSTEM] leaves the incoming configuration untouched — overriding it with the
     * value we just read from it would be a no-op at best and would fight the system at worst.
     */
    private fun wrapContextWithThemeMode(baseContext: Context?): Context? {
        if (baseContext == null) return null
        val mode = AppThemeMode.saved(baseContext)
        if (mode == AppThemeMode.SYSTEM) return baseContext

        val systemIsDark = baseContext.resources.configuration.isNightMode()
        val configuration = Configuration(baseContext.resources.configuration)
        configuration.uiMode = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
            if (mode.isDark(systemIsDark)) {
                Configuration.UI_MODE_NIGHT_YES
            } else {
                Configuration.UI_MODE_NIGHT_NO
            }
        return baseContext.createConfigurationContext(configuration)
    }

    /** The palette this activity's Compose content should use. */
    protected fun currentPalette() =
        AppThemeMode.saved(this).palette(resources.configuration.isNightMode())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
}
