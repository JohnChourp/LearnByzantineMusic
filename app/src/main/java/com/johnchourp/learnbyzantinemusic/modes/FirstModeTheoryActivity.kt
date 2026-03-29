package com.johnchourp.learnbyzantinemusic.modes

import android.os.Bundle
import android.widget.Button
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.R

class FirstModeTheoryActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_first_mode_theory)

        findViewById<Button>(R.id.first_mode_theory_back_button).setOnClickListener {
            finish()
        }
    }
}
