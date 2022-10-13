package com.example.learnbyzantinemusic;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView LearnBuzantineMusicWithLessons_txv = findViewById(R.id.LearnBuzantineMusicWithLessons_id);
        Button Lesson_1_btn = findViewById(R.id.Lesson_1_id);

        Lesson_1_btn.setOnClickListener(v -> openLesson1());
    }




    private void openLesson1() {
        Intent intent = new Intent(this, Lesson1.class);
        startActivity(intent);
    }


}