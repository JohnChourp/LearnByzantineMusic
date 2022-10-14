package com.example.learnbyzantinemusic;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.widget.ImageView;
import android.widget.TextView;

public class Lesson1 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesson1);

        TextView Lesson_1_txv = findViewById(R.id.Lesson_1_id);
        TextView PhthongNames_txv = findViewById(R.id.PhthongNames_id);
        TextView Phthongs_txv = findViewById(R.id.Phthongs_id);
        ImageView phthongs_staircase_img = findViewById(R.id.phthongs_staircase_id);
        TextView Testimonials_txv = findViewById(R.id.Testimonials_id);
        TextView Testimonials_definition_txv = findViewById(R.id.Testimonials_definition_id);
        TextView Testimonials_examples_txv = findViewById(R.id.Testimonials_examples_id);
        TextView matching_phthongs_with_testimonials_txv = findViewById(R.id.matching_phthongs_with_testimonials_id);
        TextView QuantityCharacters_txv = findViewById(R.id.QuantityCharacters_id);
        TextView QuantityCharacters_definition_txv = findViewById(R.id.QuantityCharacters_definition_id);
        ImageView ison_img = findViewById(R.id.ison_id);
    }
}