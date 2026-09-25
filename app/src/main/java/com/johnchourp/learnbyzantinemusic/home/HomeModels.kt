package com.johnchourp.learnbyzantinemusic.home

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.anastasimatarion.AnastasimatarionLabels
import com.johnchourp.learnbyzantinemusic.calendar.LiturgicalToneKind
import com.johnchourp.learnbyzantinemusic.calendar.WeeklyToneAnnouncement

/** Color family used to tint a [HomeTile]'s icon badge so sections read as visually distinct groups. */
enum class TileAccent { Gold, Blue, Purple, Orange, Green, Brown }

/** Optional informational block rendered inside a section (e.g. the quantity-character explainer). */
enum class HomeInfo { QuantityVoices }

/** A single navigation entry on the home screen. */
data class HomeTile(
    val id: String,
    @param:StringRes val titleRes: Int,
    @param:StringRes val subtitleRes: Int?,
    val icon: ImageVector,
    val accent: TileAccent,
    val onClick: () -> Unit,
)

/** A titled group of navigation tiles, optionally preceded by an [HomeInfo] block. */
data class HomeSection(
    @param:StringRes val titleRes: Int,
    @param:StringRes val subtitleRes: Int?,
    val tiles: List<HomeTile>,
    val info: HomeInfo? = null,
)

/**
 * State of the "from scratch" guided path card.
 *
 * [stepNumber] is the position of the next unfinished step, while [completedCount] counts every
 * finished step. They differ when the learner jumped ahead and came back — both are shown, because
 * claiming one when the other is true would be a lie about their own progress.
 */
data class LearningPathUi(
    val stepNumber: Int,
    val totalSteps: Int,
    val completedCount: Int,
    @param:StringRes val nextTitleRes: Int,
    val onContinue: () -> Unit,
)

/**
 * State of the «tone of the week» card at the top of home (ClickUp `869f5x24r`).
 *
 * Built by [from] out of a [WeeklyToneAnnouncement.Announcement], so the tone comes from
 * `LiturgicalToneCycle` and nowhere else. Every kind the cycle knows has a headline: the tone of the
 * week, the day's tone in Bright Week, or — in Holy Week and the week of Pentecost — the period's
 * name and "no tone of the week", with no «Άνοιξε στους 8 Ήχους» because there is no tone to open.
 */
data class WeeklyToneUi(
    /** «Αυτή την εβδομάδα: %1$s», or the period's own label. */
    @param:StringRes val headlineRes: Int,
    /** The tone's name, argument of [headlineRes]; null for the labels that take none. */
    @param:StringRes val headlineToneRes: Int?,
    /** The tone tonight's vespers begins, when it differs from today's; null hides the line. */
    @param:StringRes val vespersToneRes: Int?,
    /** Opens the current tone in the 8 Ήχοι, one-shot; null hides the button. */
    val onOpenEightModes: (() -> Unit)?,
    val onOpenAnastasimatarion: () -> Unit,
) {
    companion object {
        /**
         * The current tone — tonight's once announced, else today's — is what «Άνοιξε στους 8 Ήχους»
         * opens, as a mode KEY: the 8 Ήχοι list is in genus order, so a tone index would name the
         * wrong mode there. [openEightModes] receives that key.
         */
        fun from(
            announcement: WeeklyToneAnnouncement.Announcement,
            openEightModes: (modeKey: String) -> Unit,
            openAnastasimatarion: () -> Unit,
        ): WeeklyToneUi {
            val today = announcement.today
            val headlineRes = when (today.kind) {
                LiturgicalToneKind.WEEKLY -> R.string.home_weekly_tone_this_week
                LiturgicalToneKind.BRIGHT_WEEK_DAY -> R.string.weekly_mode_calendar_tone_bright_week
                LiturgicalToneKind.HOLY_WEEK -> R.string.weekly_mode_calendar_tone_none_holy_week
                LiturgicalToneKind.PENTECOST_WEEK -> R.string.weekly_mode_calendar_tone_none_pentecost_week
            }
            val modeKey = announcement.currentToneIndex?.let { AnastasimatarionLabels.MODE_ORDER[it] }
            return WeeklyToneUi(
                headlineRes = headlineRes,
                headlineToneRes = today.toneNameRes,
                vespersToneRes = announcement.fromVespers?.toneNameRes,
                onOpenEightModes = modeKey?.let { key -> { openEightModes(key) } },
                onOpenAnastasimatarion = openAnastasimatarion,
            )
        }
    }
}
