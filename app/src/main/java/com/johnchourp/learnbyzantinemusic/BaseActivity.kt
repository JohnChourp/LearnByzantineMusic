package com.johnchourp.learnbyzantinemusic

import android.content.Context
import android.content.res.Configuration
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.johnchourp.learnbyzantinemusic.ui.theme.AppThemeMode
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPalette

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
 * **System bars (targetSdk 36).** From Android 15 every app is drawn edge-to-edge: behind the status
 * bar, the navigation bar and the keyboard, with no way to opt out from Android 16. No screen of the
 * app handles insets (none uses a Scaffold), so instead of 26 screens each padding themselves,
 * [applySystemBarInsets] does it once here: the content view keeps out of the bars, the display
 * cutout and the keyboard — the same layout the app had at targetSdk 34 — and [enableEdgeToEdge]
 * colours the bar icons for the in-app theme (its night flag is set in [attachBaseContext]).
 * `EveryScreenStaysInsideTheSystemBarsTest` pins this (ClickUp `869fhmxpt`).
 *
 * Reads `app_language_code`, `app_font_step`, `app_theme_mode` — see
 * [com.johnchourp.learnbyzantinemusic.prefs.AppPrefs]. Writes only once, through
 * [AppThemeMode.saved], to settle the «Όπως η συσκευή» value that v1.17.4 retired.
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
     * Every choice sets the flag: since v1.17.4 none of them follows the device ([AppThemeMode]).
     */
    private fun wrapContextWithThemeMode(baseContext: Context?): Context? {
        if (baseContext == null) return null
        val mode = AppThemeMode.saved(baseContext)
        val configuration = Configuration(baseContext.resources.configuration)
        configuration.uiMode = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
            if (mode.isDark) {
                Configuration.UI_MODE_NIGHT_YES
            } else {
                Configuration.UI_MODE_NIGHT_NO
            }
        return baseContext.createConfigurationContext(configuration)
    }

    /** The palette this activity's Compose content should use. */
    protected fun currentPalette() = AppThemeMode.saved(this).palette

    /** The orientation this screen is held to: portrait, except where a screen overrides it. */
    protected open val screenOrientation: Int get() = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

    override fun onCreate(savedInstanceState: Bundle?) {
        val palette = currentPalette()
        enableEdgeToEdgeFor(palette)
        super.onCreate(savedInstanceState)
        requestedOrientation = screenOrientation
        applySystemBarInsets(findViewById(android.R.id.content))
        paintSystemBars(palette)
    }

    /**
     * Colours the bars and the strip under them for [palette]: the page colour, with icons light on
     * a dark page and dark on a light one. Built from the palette rather than `SystemBarStyle.auto`,
     * so a three-button navigation bar shows the page instead of a translucent scrim. A screen whose
     * page colour changes while it is open — the lectern reader's night tint — calls this again.
     */
    protected fun paintSystemBars(palette: LbmPalette) {
        enableEdgeToEdgeFor(palette)
        findViewById<View>(android.R.id.content).setBackgroundColor(palette.pageBg.toArgb())
    }

    private fun enableEdgeToEdgeFor(palette: LbmPalette) {
        val page = palette.pageBg.toArgb()
        // API 24–25 cannot draw dark navigation icons; there a light page gets the default dark scrim.
        val style = if (palette.isDark) SystemBarStyle.dark(page) else SystemBarStyle.light(page, LIGHT_PAGE_DARK_SCRIM)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    /**
     * Pads the content view by the system bars, the display cutout and the keyboard, and consumes
     * those insets, so every screen lays out exactly as it did before edge-to-edge. A bar that is
     * hidden reports 0 and costs no space.
     */
    private fun applySystemBarInsets(content: View) {
        // The padding below the transparent bars shows this view's background, which paintSystemBars
        // sets to the page colour, so the page simply continues under the bars.
        ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.displayCutout() or
                    WindowInsetsCompat.Type.ime(),
            )
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
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

/** The scrim behind the navigation bar on a light page where Android cannot draw dark icons (API 24–25). */
private const val LIGHT_PAGE_DARK_SCRIM = 0x801B1B1B.toInt()
