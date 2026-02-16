package com.johnchourp.learnbyzantinemusic

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.johnchourp.learnbyzantinemusic.calendar.WeeklyModeCalendarActivity
import com.johnchourp.learnbyzantinemusic.modes.EightModesActivity

class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_main_activity) // Set the layout resource ID
        val phthongsNamesBtn = findViewById<Button>(R.id.phthongs_names_btn)
        val duotrioquatroBtn = findViewById<Button>(R.id.duotrioquatro_btn)
        val climbingCompositionsBtn = findViewById<Button>(R.id.climbing_compositions_btn)

        val ascentsBtn = findViewById<Button>(R.id.ascents_btn)
        val descentsBtn = findViewById<Button>(R.id.descents_btn)

        val qualityBtn = findViewById<Button>(R.id.quality_btn)
        val timeBtn = findViewById<Button>(R.id.time_btn)


        val testimoniesBtn = findViewById<Button>(R.id.testimonies_btn)
        val eightModesBtn = findViewById<Button>(R.id.eight_modes_btn)
        val calendarBtn = findViewById<Button>(R.id.calendar_btn)
        val settingsBtn = findViewById<Button>(R.id.settings_btn)
        val poweredByTextView = findViewById<TextView>(R.id.powered_by_text_view)

        poweredByTextView.text =
            getString(R.string.powered_by_version, BuildConfig.VERSION_NAME)

        phthongsNamesBtn.setOnClickListener { openPhthongsNames() }
        duotrioquatroBtn.setOnClickListener { openDuotrioquatro() }
        climbingCompositionsBtn.setOnClickListener { openClimbingCompositions() }

        ascentsBtn.setOnClickListener { openAscents() }
        descentsBtn.setOnClickListener { openDescents() }

        qualityBtn.setOnClickListener { openQuality() }
        timeBtn.setOnClickListener { openTime() }

        testimoniesBtn.setOnClickListener { openTestimonies() }
        eightModesBtn.setOnClickListener { openEightModes() }
        calendarBtn.setOnClickListener { openWeeklyModeCalendar() }
        settingsBtn.setOnClickListener { openSettings() }
    }

    private fun openPhthongsNames() {
        val intent = Intent(this, com.johnchourp.learnbyzantinemusic.lessons.PhthongsNames::class.java)
        startActivity(intent)
    }

    private fun openDuotrioquatro() {
        val intent = Intent(this, com.johnchourp.learnbyzantinemusic.lessons.Duotrioquatro::class.java)
        startActivity(intent)
    }

    private fun openClimbingCompositions() {
        val intent = Intent(this, com.johnchourp.learnbyzantinemusic.lessons.ClimbingCompositions::class.java)
        startActivity(intent)
    }

    private fun openAscents() {
        val intent =
            Intent(this, com.johnchourp.learnbyzantinemusic.summary_theory.Ascents::class.java)
        startActivity(intent)
    }

    private fun openDescents() {
        val intent =
            Intent(this, com.johnchourp.learnbyzantinemusic.summary_theory.Descents::class.java)
        startActivity(intent)
    }

    private fun openQuality() {
        val intent =
            Intent(this, com.johnchourp.learnbyzantinemusic.summary_theory.Quality::class.java)
        startActivity(intent)
    }

    private fun openTime() {
        val intent = Intent(this, com.johnchourp.learnbyzantinemusic.summary_theory.Time::class.java)
        startActivity(intent)
    }

    private fun openTestimonies() {
        val intent = Intent(this, com.johnchourp.learnbyzantinemusic.summary_theory.Testimonies::class.java)
        startActivity(intent)
    }

    private fun openEightModes() {
        val intent = Intent(this, EightModesActivity::class.java)
        startActivity(intent)
    }

    private fun openWeeklyModeCalendar() {
        val intent = Intent(this, WeeklyModeCalendarActivity::class.java)
        startActivity(intent)
    }

    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }
}
