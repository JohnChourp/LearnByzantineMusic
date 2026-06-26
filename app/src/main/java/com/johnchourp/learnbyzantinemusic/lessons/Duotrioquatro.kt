package com.johnchourp.learnbyzantinemusic.lessons

import android.os.Bundle
import androidx.activity.compose.setContent
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.lessons.ui.DuotrioquatroScreen
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTheme

class Duotrioquatro : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LbmTheme {
                DuotrioquatroScreen(onBack = ::finish)
            }
        }
    }
}
