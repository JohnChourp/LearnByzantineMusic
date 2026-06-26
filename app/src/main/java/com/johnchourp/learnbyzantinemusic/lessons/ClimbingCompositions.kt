package com.johnchourp.learnbyzantinemusic.lessons

import android.os.Bundle
import androidx.activity.compose.setContent
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.lessons.ui.ClimbingCompositionsScreen
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTheme

class ClimbingCompositions : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LbmTheme {
                ClimbingCompositionsScreen(onBack = ::finish)
            }
        }
    }
}
