package com.johnchourp.learnbyzantinemusic

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stairs
import androidx.compose.material.icons.filled.Timer
import com.johnchourp.learnbyzantinemusic.calendar.WeeklyModeCalendarActivity
import com.johnchourp.learnbyzantinemusic.home.HomeInfo
import com.johnchourp.learnbyzantinemusic.home.HomeScreen
import com.johnchourp.learnbyzantinemusic.home.HomeSection
import com.johnchourp.learnbyzantinemusic.home.HomeTile
import com.johnchourp.learnbyzantinemusic.home.TileAccent
import com.johnchourp.learnbyzantinemusic.modes.EightModesActivity
import com.johnchourp.learnbyzantinemusic.notes.NotesActivity
import com.johnchourp.learnbyzantinemusic.recordings.RecordingsActivity
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTheme

class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LbmTheme {
                HomeScreen(
                    title = getString(R.string.learn_byzantine_music),
                    subtitle = getString(R.string.home_subtitle),
                    version = BuildConfig.VERSION_NAME,
                    sections = remember { buildHomeSections() },
                )
            }
        }

        maybeShowLanguageOnboarding()
    }

    private fun buildHomeSections(): List<HomeSection> = listOf(
        HomeSection(
            titleRes = R.string.home_section_phthongs,
            subtitleRes = R.string.home_section_phthongs_sub,
            tiles = listOf(
                HomeTile(
                    id = "phthongs_names",
                    titleRes = R.string.phthongs_names,
                    subtitleRes = R.string.home_tile_phthongs_names_sub,
                    icon = Icons.Filled.MusicNote,
                    accent = TileAccent.Gold,
                    onClick = ::openPhthongsNames,
                ),
                HomeTile(
                    id = "duotrioquatro",
                    titleRes = R.string.duotrioquatro,
                    subtitleRes = R.string.home_tile_duotrioquatro_sub,
                    icon = Icons.Filled.Timer,
                    accent = TileAccent.Gold,
                    onClick = ::openDuotrioquatro,
                ),
            ),
        ),
        HomeSection(
            titleRes = R.string.quantity_characters,
            subtitleRes = null,
            info = HomeInfo.QuantityVoices,
            tiles = listOf(
                HomeTile(
                    id = "ascents",
                    titleRes = R.string.ascents,
                    subtitleRes = R.string.home_tile_ascents_sub,
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    accent = TileAccent.Blue,
                    onClick = ::openAscents,
                ),
                HomeTile(
                    id = "descents",
                    titleRes = R.string.descents,
                    subtitleRes = R.string.home_tile_descents_sub,
                    icon = Icons.AutoMirrored.Filled.TrendingDown,
                    accent = TileAccent.Blue,
                    onClick = ::openDescents,
                ),
                HomeTile(
                    id = "climbing_compositions",
                    titleRes = R.string.climbing_compositions,
                    subtitleRes = R.string.home_tile_climbing_sub,
                    icon = Icons.Filled.Stairs,
                    accent = TileAccent.Blue,
                    onClick = ::openClimbingCompositions,
                ),
            ),
        ),
        HomeSection(
            titleRes = R.string.characters,
            subtitleRes = R.string.home_section_characters_sub,
            tiles = listOf(
                HomeTile(
                    id = "quality",
                    titleRes = R.string.quality,
                    subtitleRes = R.string.home_tile_quality_sub,
                    icon = Icons.Filled.AutoAwesome,
                    accent = TileAccent.Purple,
                    onClick = ::openQuality,
                ),
                HomeTile(
                    id = "time",
                    titleRes = R.string.time,
                    subtitleRes = R.string.home_tile_time_sub,
                    icon = Icons.Filled.Schedule,
                    accent = TileAccent.Purple,
                    onClick = ::openTime,
                ),
            ),
        ),
        HomeSection(
            titleRes = R.string.home_section_testimonies_modes,
            subtitleRes = null,
            tiles = listOf(
                HomeTile(
                    id = "testimonies",
                    titleRes = R.string.testimonies,
                    subtitleRes = R.string.home_tile_testimonies_sub,
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    accent = TileAccent.Brown,
                    onClick = ::openTestimonies,
                ),
                HomeTile(
                    id = "eight_modes",
                    titleRes = R.string.eight_modes_open,
                    subtitleRes = R.string.home_tile_eight_modes_sub,
                    icon = Icons.Filled.LibraryMusic,
                    accent = TileAccent.Brown,
                    onClick = ::openEightModes,
                ),
            ),
        ),
        HomeSection(
            titleRes = R.string.home_section_practice,
            subtitleRes = R.string.home_section_practice_sub,
            tiles = listOf(
                HomeTile(
                    id = "melody_trainer",
                    titleRes = R.string.melody_trainer_open,
                    subtitleRes = R.string.home_tile_melody_trainer_sub,
                    icon = Icons.Filled.GraphicEq,
                    accent = TileAccent.Green,
                    onClick = ::openMelodyTrainer,
                ),
                HomeTile(
                    id = "calendar",
                    titleRes = R.string.weekly_mode_calendar_open,
                    subtitleRes = R.string.home_tile_calendar_sub,
                    icon = Icons.Filled.CalendarMonth,
                    accent = TileAccent.Green,
                    onClick = ::openWeeklyModeCalendar,
                ),
                HomeTile(
                    id = "recordings",
                    titleRes = R.string.recordings_open,
                    subtitleRes = R.string.home_tile_recordings_sub,
                    icon = Icons.Filled.Mic,
                    accent = TileAccent.Orange,
                    onClick = ::openRecordings,
                ),
                HomeTile(
                    id = "notes",
                    titleRes = R.string.notes_open,
                    subtitleRes = R.string.home_tile_notes_sub,
                    icon = Icons.Filled.EditNote,
                    accent = TileAccent.Orange,
                    onClick = ::openNotes,
                ),
            ),
        ),
        HomeSection(
            titleRes = R.string.settings_button,
            subtitleRes = null,
            tiles = listOf(
                HomeTile(
                    id = "settings",
                    titleRes = R.string.settings_button,
                    subtitleRes = R.string.home_tile_settings_sub,
                    icon = Icons.Filled.Settings,
                    accent = TileAccent.Brown,
                    onClick = ::openSettings,
                ),
            ),
        ),
    )

    private fun maybeShowLanguageOnboarding() {
        if (AppLanguage.isLanguageOnboardingCompleted(this)) {
            return
        }

        showLanguageSelectionDialog()
    }

    private fun showLanguageSelectionDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.language_first_launch_title))
            .setMessage(getString(R.string.language_first_launch_message))
            .setCancelable(false)
            .setPositiveButton(AppLanguage.getNativeLanguageName(AppLanguage.languageGreek)) { _, _ ->
                showLanguageConfirmationDialog(AppLanguage.languageGreek, isOnboardingFlow = true)
            }
            .setNegativeButton(AppLanguage.getNativeLanguageName(AppLanguage.languageEnglish)) { _, _ ->
                showLanguageConfirmationDialog(AppLanguage.languageEnglish, isOnboardingFlow = true)
            }
            .show()
    }

    private fun showLanguageConfirmationDialog(targetLanguageCode: String, isOnboardingFlow: Boolean) {
        val languageName = AppLanguage.getNativeLanguageName(targetLanguageCode)
        val title = AppLanguage.getLocalizedString(
            context = this,
            languageCode = targetLanguageCode,
            stringRes = R.string.language_change_confirm_title,
        )
        val message = AppLanguage.getLocalizedString(
            this,
            targetLanguageCode,
            R.string.language_change_confirm_message,
            languageName,
        )
        val acceptLabel = AppLanguage.getLocalizedString(
            context = this,
            languageCode = targetLanguageCode,
            stringRes = R.string.language_change_confirm_accept,
        )
        val cancelLabel = AppLanguage.getLocalizedString(
            context = this,
            languageCode = targetLanguageCode,
            stringRes = R.string.language_change_confirm_cancel,
        )

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(!isOnboardingFlow)
            .setPositiveButton(acceptLabel) { _, _ ->
                AppLanguage.saveLanguageCode(this, targetLanguageCode)
                AppLanguage.setLanguageOnboardingCompleted(this, true)
                restartToMainActivity()
            }
            .setNegativeButton(cancelLabel) { _, _ ->
                if (isOnboardingFlow) {
                    showLanguageSelectionDialog()
                }
            }
            .show()
    }

    private fun restartToMainActivity() {
        val restartIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(restartIntent)
        finish()
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

    private fun openMelodyTrainer() {
        val intent = Intent(this, com.johnchourp.learnbyzantinemusic.trainer.MelodyTrainerActivity::class.java)
        startActivity(intent)
    }

    private fun openWeeklyModeCalendar() {
        val intent = Intent(this, WeeklyModeCalendarActivity::class.java)
        startActivity(intent)
    }

    private fun openRecordings() {
        val intent = Intent(this, RecordingsActivity::class.java)
        startActivity(intent)
    }

    private fun openNotes() {
        val intent = Intent(this, NotesActivity::class.java)
        startActivity(intent)
    }

    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }
}
