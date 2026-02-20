package com.johnchourp.learnbyzantinemusic

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity

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
