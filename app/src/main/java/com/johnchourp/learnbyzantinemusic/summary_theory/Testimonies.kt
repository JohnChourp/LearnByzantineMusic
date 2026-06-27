package com.johnchourp.learnbyzantinemusic.summary_theory

import android.os.Bundle
import androidx.activity.compose.setContent
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.summary_theory.ui.TestimoniesScreen
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTheme

class Testimonies : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LbmTheme {
                TestimoniesScreen(onBack = ::finish)
            }
        }
    }
}
