package com.johnchourp.learnbyzantinemusic

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * The base every screen in the app extends, and the reason two global settings work everywhere
 * without a single screen knowing about them.
 *
 * **Flow.** `attachBaseContext` wraps the incoming context twice before Android inflates anything:
 * first with the saved language, then with the saved font scale. Because this happens at attach time
 * rather than in `onCreate`, resources resolve in the right locale and at the right scale from the
 * very first layout pass — no screen re-reads a preference, and none can forget to.
 *
 * **Order matters.** Locale first, font scale second: the font-scale wrapper is built on top of the
 * localised configuration, so it cannot drop the locale it was handed.
 *
 * **Also.** Locks every screen to portrait. The app is meant to be read off a stand while chanting,
 * and several diagrams assume portrait width.
 *
 * Reads (never writes): `app_language_code`, `app_font_step` — see
 * [com.johnchourp.learnbyzantinemusic.prefs.AppPrefs].
 */
abstract class BaseActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context?) {
        val languageContext = AppLanguage.wrapContextWithLocale(newBase)
        val fontScaleContext = AppFontScale.wrapContextWithFontScale(languageContext)
        super.attachBaseContext(fontScaleContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
}
