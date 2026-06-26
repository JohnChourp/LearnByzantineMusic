package com.johnchourp.learnbyzantinemusic.summary_theory

import android.os.Bundle
import androidx.activity.compose.setContent
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.summary_theory.ui.AscentsScreen
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTheme

class Ascents : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LbmTheme {
                AscentsScreen(onBack = ::finish)
            }
        }
    }
}
