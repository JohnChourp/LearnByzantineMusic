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
        val lesson4Btn = findViewById<Button>(R.id.lesson4_btn)
        val lesson5Btn = findViewById<Button>(R.id.lesson5_btn)

        val ascentsBtn = findViewById<Button>(R.id.ascents_btn)
        val descentsBtn = findViewById<Button>(R.id.descents_btn)
        val transcendentAscentsBtn = findViewById<Button>(R.id.transcendent_ascents_btn)
        val transcendentDescentsBtn = findViewById<Button>(R.id.transcendent_descents_btn)

        lesson1Btn.setOnClickListener { openLesson1() }
        lesson2Btn.setOnClickListener { openLesson2() }
        lesson3Btn.setOnClickListener { openLesson3() }
        lesson4Btn.setOnClickListener { openLesson4() }
        lesson5Btn.setOnClickListener { openLesson5() }

        ascentsBtn.setOnClickListener { openAscents() }
        descentsBtn.setOnClickListener { openDescents() }
        transcendentAscentsBtn.setOnClickListener { openTranscendentAscents() }
        transcendentDescentsBtn.setOnClickListener { openTranscendentDescents() }
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

    private fun openLesson4() {
        val intent = Intent(this, com.example.learnbyzantinemusic.lessons.Lesson4::class.java)
        startActivity(intent)
    }

    private fun openLesson5() {
        val intent = Intent(this, com.example.learnbyzantinemusic.lessons.Lesson5::class.java)
        startActivity(intent)
    }

    private fun openAscents() {
        val intent = Intent(this, com.example.learnbyzantinemusic.summary_theory.Ascents::class.java)
        startActivity(intent)
    }

    private fun openDescents() {
        val intent = Intent(this, com.example.learnbyzantinemusic.summary_theory.Descents::class.java)
        startActivity(intent)
    }

    private fun openTranscendentAscents() {
        val intent = Intent(this, com.example.learnbyzantinemusic.summary_theory.TranscendentAscents::class.java)
        startActivity(intent)
    }

    private fun openTranscendentDescents() {
        val intent = Intent(this, com.example.learnbyzantinemusic.summary_theory.TranscendentDescents::class.java)
        startActivity(intent)
    }
}
