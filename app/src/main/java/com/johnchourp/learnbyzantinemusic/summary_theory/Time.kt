package com.johnchourp.learnbyzantinemusic.summary_theory

import android.os.Bundle
import androidx.activity.compose.setContent
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.summary_theory.ui.TimeScreen
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTheme

class Time : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LbmTheme {
                TimeScreen(onBack = ::finish)
            }
        }
    }
}
