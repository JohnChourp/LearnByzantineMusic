package com.johnchourp.learnbyzantinemusic.modes

import androidx.annotation.StringRes
import com.johnchourp.learnbyzantinemusic.R

data class TheoryTopic(
    val key: String,
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int = 0
)

object TheoryTopicCatalog {
    const val EXTRA_TOPIC_KEY = "theory_topic_key"

    const val OCTAVE_DIAPASON = "octave_diapason"
    const val MIDDLE_ACUTE_GRAVE_DIAPASON = "middle_acute_grave_diapason"
    const val SCALE = "scale"
    const val TESTIMONIES = "testimonies"
    const val PENTACHORD_TROCHOS = "pentachord_trochos"

    val topics: List<TheoryTopic> = listOf(
        TheoryTopic(
            key = OCTAVE_DIAPASON,
            titleRes = R.string.theory_topic_octave_diapason_title,
            bodyRes = R.string.theory_topic_octave_diapason_body
        ),
        TheoryTopic(
            key = MIDDLE_ACUTE_GRAVE_DIAPASON,
            titleRes = R.string.theory_topic_middle_acute_grave_diapason_title
        ),
        TheoryTopic(
            key = SCALE,
            titleRes = R.string.theory_topic_scale_title
        ),
        TheoryTopic(
            key = TESTIMONIES,
            titleRes = R.string.theory_topic_testimonies_title
        ),
        TheoryTopic(
            key = PENTACHORD_TROCHOS,
            titleRes = R.string.theory_topic_pentachord_trochos_title
        )
    )

    fun byKey(key: String?): TheoryTopic =
        topics.firstOrNull { it.key == key } ?: topics.first()
}
