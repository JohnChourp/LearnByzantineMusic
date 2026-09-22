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
import androidx.compose.runtime.mutableStateOf
import com.johnchourp.learnbyzantinemusic.home.LearningPathUi
import com.johnchourp.learnbyzantinemusic.home.HomeSection
import com.johnchourp.learnbyzantinemusic.home.HomeTile
import com.johnchourp.learnbyzantinemusic.learning.LearningPath
import com.johnchourp.learnbyzantinemusic.learning.LearningProgress
import com.johnchourp.learnbyzantinemusic.home.TileAccent
import com.johnchourp.learnbyzantinemusic.modes.EightModesActivity
import com.johnchourp.learnbyzantinemusic.modes.EightModesNavigation
import com.johnchourp.learnbyzantinemusic.notes.NotesActivity
import com.johnchourp.learnbyzantinemusic.recordings.RecordingsActivity
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTheme

/**
 * The home screen: the catalogue of everything the app offers, plus the "where was I" card.
 *
 * **Flow.** [buildHomeSections] declares the six sections — Φθόγγοι, Ανιόντες/Κατιόντες, Χαρακτήρες,
 * Μαρτυρίες & Ήχοι, Εξάσκηση, Ρυθμίσεις — in pedagogical order; that order *is* the beginner's path,
 * not a separate list. `withProgressTracking` wraps each tile's click so opening a lesson records it
 * as done before the target activity starts, and `learningPathUi` turns the recorded ids into the
 * card at the top. On first launch, `maybeShowLanguageOnboarding` asks for a language before anything
 * else.
 *
 * **Why progress is re-read in `onResume`.** The learner finishes a lesson and presses back; the card
 * has to have advanced by the time they see it again. Reading only in `onCreate` would leave it stale
 * until the process restarted.
 *
 * **Inputs:** none — this is the launcher entry point.
 * **Opens:** every other screen, by explicit Intent.
 * **Touches:** `learning_completed_step_ids` (read + write), `app_language_code` and
 * `app_language_onboarding_completed` (read + write, through the onboarding dialogs).
 *
 * The path never hides or reorders the sections below it: an experienced chanter ignores the card and
 * taps straight through.
 */
class MainActivity : BaseActivity() {
    /** Re-read in [onResume] so the card advances after the learner comes back from a lesson. */
    private val completedSteps = mutableStateOf<Set<String>>(emptySet())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        completedSteps.value = LearningProgress.completedSteps(this)
        setContent {
            LbmTheme {
                val sections = remember { withProgressTracking(buildHomeSections()) }
                HomeScreen(
                    title = getString(R.string.learn_byzantine_music),
                    subtitle = getString(R.string.home_subtitle),
                    version = BuildConfig.VERSION_NAME,
                    sections = sections,
                    learningPath = learningPathUi(sections, completedSteps.value),
                    // canOpenEightModesHome: from here the «8 Ήχοι» row must actually navigate.
                    // Inside that screen it is where you already are, so it does nothing there.
                    onOpenSearch = {
                        EightModesNavigation.showMenu(
                            activity = this,
                            selectedTopicKey = null,
                            canOpenEightModesHome = true,
                        )
                    },
                )
            }
        }

        maybeShowLanguageOnboarding()
    }

    override fun onResume() {
        super.onResume()
        completedSteps.value = LearningProgress.completedSteps(this)
    }

    /**
     * Wraps the click of every tile that is a path step so opening the lesson records it.
     * Driven by the tile id, so no tile has to be edited by hand and a tile that leaves the
     * path stops being tracked on its own.
     */
    private fun withProgressTracking(sections: List<HomeSection>): List<HomeSection> =
        sections.map { section ->
            section.copy(
                tiles = section.tiles.map { tile ->
                    if (!LearningPath.isStep(tile.id)) {
                        tile
                    } else {
                        tile.copy(
                            onClick = {
                                LearningProgress.markCompleted(this, tile.id)
                                completedSteps.value = LearningProgress.completedSteps(this)
                                tile.onClick()
                            },
                        )
                    }
                },
            )
        }

    /**
     * Null once every step is done, or if a path id has no tile on the screen — in both cases the
     * card disappears rather than rendering a dead or wrong state.
     *
     * [sections] must already be progress-tracked, so "Continue" records the step too.
     */
    private fun learningPathUi(
        sections: List<HomeSection>,
        completed: Set<String>,
    ): LearningPathUi? {
        val nextId = LearningPath.nextStepId(completed) ?: return null
        val tile = sections.firstNotNullOfOrNull { section ->
            section.tiles.firstOrNull { it.id == nextId }
        } ?: return null
        return LearningPathUi(
            stepNumber = LearningPath.positionOf(nextId),
            totalSteps = LearningPath.size,
            completedCount = LearningPath.completedCount(completed),
            nextTitleRes = tile.titleRes,
            onContinue = tile.onClick,
        )
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
