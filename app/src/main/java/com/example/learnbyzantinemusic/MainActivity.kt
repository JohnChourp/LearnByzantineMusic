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
        val lesson2Btn = findViewById<Button>(R.id.lesson2_btn)
        val lesson3Btn = findViewById<Button>(R.id.lesson3_btn)

        lesson1Btn.setOnClickListener { openLesson1() }
        lesson2Btn.setOnClickListener { openLesson2() }
        lesson3Btn.setOnClickListener { openLesson3() }
    }

    private fun openLesson1() {
        val intent = Intent(this, com.example.learnbyzantinemusic.lessons.Lesson1::class.java)
        startActivity(intent)
    }

    private fun openLesson2() {
        val intent = Intent(this, com.example.learnbyzantinemusic.lessons.Lesson2::class.java)
        startActivity(intent)
    }

    private fun openLesson3() {
        val intent = Intent(this, com.example.learnbyzantinemusic.lessons.Lesson3::class.java)
        startActivity(intent)
    }
}
