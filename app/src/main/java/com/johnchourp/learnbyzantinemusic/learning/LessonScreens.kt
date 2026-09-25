package com.johnchourp.learnbyzantinemusic.learning

import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.lessons.ClimbingCompositions
import com.johnchourp.learnbyzantinemusic.lessons.Duotrioquatro
import com.johnchourp.learnbyzantinemusic.lessons.PhthongsNames
import com.johnchourp.learnbyzantinemusic.modes.EightModesActivity
import com.johnchourp.learnbyzantinemusic.summary_theory.Ascents
import com.johnchourp.learnbyzantinemusic.summary_theory.Descents
import com.johnchourp.learnbyzantinemusic.summary_theory.Quality
import com.johnchourp.learnbyzantinemusic.summary_theory.Testimonies
import com.johnchourp.learnbyzantinemusic.summary_theory.Time

/**
 * Each lesson's title and screen, by its home-tile id — the ids of [LearningPath].
 *
 * This used to be private to `MainActivity`, one `openX()` per tile, so nothing else could open
 * «the next lesson». The daily practice session needs exactly that (ClickUp `869f5x2dy`), and the
 * home tiles and the session must never disagree about which page a lesson is or what it is called,
 * so both read it here: the home builds its lesson tiles from [titleRes] and [intent].
 *
 * Each entry builds its Intent only when asked, so reading [ids] loads no Activity class, and a JVM
 * test can prove every path step has a screen.
 */
object LessonScreens {

    private class Lesson(@StringRes val titleRes: Int, val open: (Context) -> Intent)

    private val lessons: Map<String, Lesson> = mapOf(
        "phthongs_names" to Lesson(R.string.phthongs_names) { Intent(it, PhthongsNames::class.java) },
        "duotrioquatro" to Lesson(R.string.duotrioquatro) { Intent(it, Duotrioquatro::class.java) },
        "ascents" to Lesson(R.string.ascents) { Intent(it, Ascents::class.java) },
        "descents" to Lesson(R.string.descents) { Intent(it, Descents::class.java) },
        "climbing_compositions" to Lesson(R.string.climbing_compositions) { Intent(it, ClimbingCompositions::class.java) },
        "quality" to Lesson(R.string.quality) { Intent(it, Quality::class.java) },
        "time" to Lesson(R.string.time) { Intent(it, Time::class.java) },
        "testimonies" to Lesson(R.string.testimonies) { Intent(it, Testimonies::class.java) },
        "eight_modes" to Lesson(R.string.eight_modes_open) { Intent(it, EightModesActivity::class.java) },
    )

    /** The lesson ids that have a screen. */
    val ids: Set<String> get() = lessons.keys

    /** The title of lesson [id]; an unknown id is a bug, so it throws. */
    @StringRes
    fun titleRes(id: String): Int = requireNotNull(lessons[id]) { "No lesson \"$id\"" }.titleRes

    /** The Intent that opens lesson [id], or null when no lesson has that id. */
    fun intent(context: Context, id: String): Intent? = lessons[id]?.open?.invoke(context)
}
