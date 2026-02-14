package com.johnchourp.learnbyzantinemusic

import android.content.Context
import androidx.activity.ComponentActivity

abstract class BaseActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(AppFontScale.wrapContextWithFontScale(newBase))
    }
}
