package com.johnchourp.learnbyzantinemusic.learning

/**
 * The ordered "from scratch" lesson path.
 *
 * The ids are [com.johnchourp.learnbyzantinemusic.home.HomeTile] ids, not a second catalog:
 * titles, icons and navigation stay owned by the home sections, so a lesson cannot drift
 * between the path and the screen it opens.
 *
 * Only teaching pages belong here. The practice tools (melody trainer, calendar, recordings,
 * notes) and settings are deliberately absent — they are things you use, not steps you finish.
 *
 * Every function here is pure so the ordering rules can be tested without Android.
 */
object LearningPath {

    val stepIds: List<String> = listOf(
        "phthongs_names",
        "duotrioquatro",
        "ascents",
        "descents",
        "climbing_compositions",
        "quality",
        "time",
        "testimonies",
        "eight_modes",
    )

    val size: Int get() = stepIds.size

    /** True when [id] is part of the path, i.e. opening it should count as progress. */
    fun isStep(id: String): Boolean = id in stepIds

    /**
     * How many path steps are done. Ids that are not in the path are ignored, so a stored
     * id left over from an older path version can never push the count past [size].
     */
    fun completedCount(completed: Set<String>): Int = stepIds.count { it in completed }

    /** The first step not yet done, or null once the whole path is finished. */
    fun nextStepId(completed: Set<String>): String? = stepIds.firstOrNull { it !in completed }

    /** 1-based position of [id] in the path, or 0 when it is not a step. */
    fun positionOf(id: String): Int = stepIds.indexOf(id) + 1
}
