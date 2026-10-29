package com.johnchourp.learnbyzantinemusic.eartrainer

import com.johnchourp.learnbyzantinemusic.music.Mode

/**
 * The three levels of «Μάντεψε τον ήχο» (ClickUp `869f4tqb9`): which ήχοι a question may be, and
 * whether the ison sounds under it.
 *
 * **The ison comes first, not last** (owner decision, 2026-10-29). It is the reference the ear
 * measures every interval against — the reason `IsonDrone` exists — so a question over the ison is
 * the easier one. The last level takes it away and leaves the learner to hear the base alone.
 *
 * [key] is stored (`ear_trainer_level`): never rename one.
 */
enum class EarTrainerLevel(val key: String, val modes: List<Mode>, val withIson: Boolean) {
    /** The four κύριοι, over the ison. */
    KYRIOI("kyrioi", listOf(Mode.FIRST, Mode.SECOND, Mode.THIRD, Mode.FOURTH), withIson = true),

    /** All eight, over the ison. */
    ALL("all", Mode.entries.toList(), withIson = true),

    /** All eight, without the ison. */
    ALL_WITHOUT_ISON("all_without_ison", Mode.entries.toList(), withIson = false);

    companion object {
        val DEFAULT = KYRIOI

        /** Boundary parser: a stored key → its level, or null when it names none. */
        fun fromKey(key: String?): EarTrainerLevel? = entries.firstOrNull { it.key == key }
    }
}
