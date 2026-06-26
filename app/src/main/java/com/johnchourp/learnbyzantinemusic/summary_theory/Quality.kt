package com.johnchourp.learnbyzantinemusic.summary_theory

import android.os.Bundle
import androidx.activity.compose.setContent
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.summary_theory.ui.QualityScreen
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTheme

class Quality : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LbmTheme {
                QualityScreen(onBack = ::finish)
            }
        }
    }
}
