package com.example.learnbyzantinemusic

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_main_activity) // Set the layout resource ID
        val lesson1Btn = findViewById<Button>(R.id.lesson1_btn)

        lesson1Btn.setOnClickListener { openLesson1() }
    }

    private fun openLesson1() {
        val intent = Intent(this, com.example.learnbyzantinemusic.lessons.lesson1::class.java)
        startActivity(intent)
    }
}
