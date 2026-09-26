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
 * **Re-applied on resume.** Applying them at attach time means a screen keeps the values it was
 * created with. Settings recreates only itself when the theme or the font size changes, so the home
 * screen waiting behind it came back in the old theme until the app was restarted — since v1.16.0,
 * reported on a device on 2026-09-26. Every screen now remembers the three values it was attached
 * with ([AttachedSettings]) and recreates itself in [onResume] when any of them has changed since.
 *
 * **Also.** Locks every screen to portrait. The app is meant to be read off a stand while chanting,
 * and several diagrams assume portrait width. The one exception is the digital lectern's reader
 * (ClickUp `869f5x2e7`), whose PDF pages are wide as often as tall: it overrides [screenOrientation].
 *
 * Reads (never writes): `app_language_code`, `app_font_step`, `app_theme_mode` — see
 * [com.johnchourp.learnbyzantinemusic.prefs.AppPrefs].
 */
abstract class BaseActivity : ComponentActivity() {
    /** The language, font size and theme this screen was attached with; compared in [onResume]. */
    private var attachedWith: AttachedSettings? = null

    override fun attachBaseContext(newBase: Context?) {
        attachedWith = newBase?.let(AttachedSettings::read)
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

    /** The orientation this screen is held to: portrait, except where a screen overrides it. */
    protected open val screenOrientation: Int get() = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = screenOrientation
    }

    override fun onResume() {
        super.onResume()
        // Changed on another screen while this one waited behind it. The values are applied only
        // at attach time, so creating the screen again is the only way to show them.
        if (attachedWith != null && AttachedSettings.read(this) != attachedWith) {
            recreate()
        }
    }
}

/** The three settings [BaseActivity] applies at attach time, as saved right now. */
internal data class AttachedSettings(
    val languageCode: String,
    val fontStep: Int,
    val themeMode: AppThemeMode,
) {
    companion object {
        fun read(context: Context) = AttachedSettings(
            languageCode = AppLanguage.getSavedLanguageCode(context),
            fontStep = AppFontScale.getSavedStep(context),
            themeMode = AppThemeMode.saved(context),
        )
    }
}
