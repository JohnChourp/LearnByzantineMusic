package com.example.learnbyzantinemusic;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button Lesson_1_btn = findViewById(R.id.Lesson_1_id);
        Button Lesson_2_btn = findViewById(R.id.Lesson_2_id);
        Button Lesson_3_btn = findViewById(R.id.Lesson_3_id);

        Lesson_1_btn.setOnClickListener(v -> openLesson1());
        Lesson_2_btn.setOnClickListener(v -> openLesson2());
        Lesson_3_btn.setOnClickListener(v -> openLesson3());
    }


    private void openLesson1() {
        Intent intent = new Intent(this, Lesson1.class);
        startActivity(intent);
    }

    private void openLesson2() {
        Intent intent = new Intent(this, Lesson2.class);
        startActivity(intent);
    }

    private void openLesson3() {
        Intent intent = new Intent(this, Lesson3.class);
        startActivity(intent);
    }
}