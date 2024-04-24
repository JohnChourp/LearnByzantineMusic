package com.example.learnbyzantinemusic

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_main_activity) // Set the layout resource ID
        val welcome_text_text_view = findViewById<TextView>(R.id.welcome_text_text_view)
        val lesson1_text_btn = findViewById<Button>(R.id.lesson1_text_btn)
        val lesson2_text_btn = findViewById<Button>(R.id.lesson2_text_btn)
    }
}
